package com.ccp.implementations.db.crud.elasticsearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpTimeDecorator;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.bulk.CcpErrorBulkEntityRecordNotFound;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpErrorCrudMultiGetSearchUnfeasible;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.especifications.db.crud.CcpUnionAllExecutor;
import com.ccp.especifications.db.utils.CcpDbRequester;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.especifications.http.CcpHttpMethods;
import com.ccp.especifications.http.CcpHttpResponseType;
import com.ccp.process.CcpFunctionThrowException;
import com.ccp.json.fields.validation.CcpJsonCommonsFields;
import java.util.stream.Stream;/**
 * Main {@code CcpCrud} and {@code CcpUnionAllExecutor} implementation for Elasticsearch.
 * Provides read operations ({@code getOneById}, {@code exists}, {@code unionAll}),
 * write operations ({@code save} with upsert via a Painless script) and removal ({@code delete}).
 */

class ElasticSearchCrud implements CcpCrud, CcpUnionAllExecutor {
	enum JsonFieldNames implements CcpJsonFieldName{
		upsert, params, source, script, lang, painless, docs
	}

	private CcpJsonRepresentation getRequestBodyToMultipleGet(Collection<CcpJsonRepresentation> jsons, CcpEntity... entities) {
		
		Set<CcpJsonRepresentation> docs = new LinkedHashSet<CcpJsonRepresentation>();
		
		for (CcpEntity entity : entities) {
			
			for (CcpJsonRepresentation json : jsons) {
				
 				CcpEntityMetaData entityDetails = entity.getEntityMetaData();
					boolean containsAllFields = json.containsAllFields(entityDetails.primaryKeyNames);

					boolean anyKeyIsMissing = false == containsAllFields;
				
				if(anyKeyIsMissing) {
					continue;
				}

				List<CcpJsonRepresentation> parametersToSearch = entity.getParametersToSearch(json)
						;
				docs.addAll(parametersToSearch);
			}
		}
		
		boolean unfeasibleMultiGetSearch = docs.isEmpty();
	
		if(unfeasibleMultiGetSearch) {
			CcpErrorCrudMultiGetSearchUnfeasible ccpErrorCrudMultiGetSearchUnfeasible = new CcpErrorCrudMultiGetSearchUnfeasible(jsons, entities);
			throw ccpErrorCrudMultiGetSearchUnfeasible;
		}
		CcpJsonRepresentation requestBody = CcpOtherConstants.EMPTY_JSON.put(JsonFieldNames.docs, docs);
		return requestBody;
	}
	
	
	public CcpJsonRepresentation getRequestBodyToMultipleGet(Set<String> ids, CcpEntity... entities) {
		List<CcpJsonRepresentation> indexAndIdDocs = new ArrayList<CcpJsonRepresentation>();
		for (CcpEntity entity : entities) {
			CcpEntityMetaData entityDetails = entity.getEntityMetaData();
			for (String id : ids) {
				CcpJsonRepresentation docWithIndex = CcpOtherConstants.EMPTY_JSON
				.put(CcpJsonCommonsFields._index, entityDetails.entityName);
				CcpJsonRepresentation docWithIndexAndId = docWithIndex
				.put(CcpJsonCommonsFields._id, id)
				;
				indexAndIdDocs.add(docWithIndexAndId);
			}
		}
		CcpJsonRepresentation requestBody = CcpOtherConstants.EMPTY_JSON.put(JsonFieldNames.docs, indexAndIdDocs);
		return requestBody;
	}
	
	public CcpJsonRepresentation getOneById(String entityName, String id) {
		String entityPath = "/" + entityName;
		String sourcePathPrefix = entityPath + "/_source/";
		String path = sourcePathPrefix + id ;
		CcpJsonRepresentation handlersFor200 = CcpOtherConstants.EMPTY_JSON.addJsonTransformer(200, CcpOtherConstants.DO_NOTHING);
		CcpErrorBulkEntityRecordNotFound ccpErrorBulkEntityRecordNotFound = new CcpErrorBulkEntityRecordNotFound(entityName, id);
		CcpFunctionThrowException ccpFunctionThrowException = new CcpFunctionThrowException(ccpErrorBulkEntityRecordNotFound);
		CcpJsonRepresentation handlers = handlersFor200.addJsonTransformer(404, ccpFunctionThrowException);
		
		CcpDbRequester dbUtils = CcpDependencyInjection.getDependency(CcpDbRequester.class);
		CcpJsonRepresentation response = dbUtils.executeHttpRequest("getOneById", path, CcpHttpMethods.GET, handlers, CcpOtherConstants.EMPTY_JSON, CcpHttpResponseType.singleRecord);
		
		return response;
	}

	public boolean exists(String entityName, String id) {
		String entityPath = "/" + entityName;
		String docPathPrefix = entityPath + "/_doc/";
		String path = docPathPrefix + id;
		CcpJsonRepresentation handlersFor200 = CcpOtherConstants.EMPTY_JSON.addJsonTransformer(200, ElasticSearchHttpStatus.OK);
	
		CcpJsonRepresentation flows = handlersFor200
				.addJsonTransformer(404,  ElasticSearchHttpStatus.NOT_FOUND);
		
		CcpDbRequester dbUtils = CcpDependencyInjection.getDependency(CcpDbRequester.class);
		CcpJsonRepresentation response = dbUtils.executeHttpRequest("exists", path, CcpHttpMethods.HEAD, flows, CcpOtherConstants.EMPTY_JSON, CcpHttpResponseType.singleRecord);
		ElasticSearchHttpStatus status = response.getAsObject(CcpJsonCommonsFields.ElasticSearchHttpStatus);
		
		boolean exists = ElasticSearchHttpStatus.OK.equals(status);
		return exists;
	}

	public CcpJsonRepresentation save(String entityName, CcpJsonRepresentation json, String id) {
		String entityPath = "/" + entityName;
		String updatePathPrefix = entityPath + "/_update/";
		String path = updatePathPrefix + id;
		String painlessName = JsonFieldNames.painless.name();
		CcpJsonRepresentation scriptWithLang = CcpOtherConstants.EMPTY_JSON
				.addToItem(JsonFieldNames.script, JsonFieldNames.lang, painlessName);
				CcpJsonRepresentation scriptWithSource = scriptWithLang
				.addToItem(JsonFieldNames.script, JsonFieldNames.source, "ctx._source.putAll(params);");
				CcpJsonRepresentation scriptWithParams = scriptWithSource
				.addToItem(JsonFieldNames.script, JsonFieldNames.params, json);

				CcpJsonRepresentation requestBody = scriptWithParams
				.put(JsonFieldNames.upsert, json)
				;
				CcpJsonRepresentation handlersFor409 = CcpOtherConstants.EMPTY_JSON
				.addJsonTransformer(409, values -> this.retrySave(entityName, json, id));
				CcpJsonRepresentation handlersFor409And201 = handlersFor409
				.addJsonTransformer(201,  ElasticSearchHttpStatus.CREATED);

				CcpJsonRepresentation handlers = handlersFor409And201
				.addJsonTransformer(200, ElasticSearchHttpStatus.OK)
				;
		
		CcpDbRequester dbUtils = CcpDependencyInjection.getDependency(CcpDbRequester.class);
		CcpJsonRepresentation response = dbUtils.executeHttpRequest("createOrUpdate", path, CcpHttpMethods.POST, handlers, requestBody, CcpHttpResponseType.singleRecord);
		return response;
	}

	/**
	 * Interprets the Elasticsearch {@code _update} response to tell an insert apart from an
	 * update. The two signals carried by the response are checked, in this order:
	 * <ul>
	 * <li>the body's {@code result} field, which carries the document-level outcome: {@code created}
	 * when the {@code upsert} inserted the document, {@code updated} when the script changed an
	 * existing document and {@code noop} when the document already had the content sent;</li>
	 * <li>the HTTP status, recorded in the json by the {@code save} handlers ({@code 201} becomes
	 * {@code CREATED} on insert and {@code 200} becomes {@code OK} on update), used when the body
	 * did not carry the outcome. Any other unmapped status never gets here, because
	 * {@code CcpHttpHandler} throws {@code CcpErrorHttp} first.</li>
	 * </ul>
	 */
	public boolean isInsertedDocument(CcpJsonRepresentation saveResponse) {

		String result = saveResponse.getAsString(CcpJsonCommonsFields.result);

		boolean bodySaysItWasInserted = "created".equals(result);

		if(bodySaysItWasInserted) {
			return true;
		}

		boolean bodySaysItWasUpdated = "updated".equals(result);
		boolean bodySaysItWasUnchanged = "noop".equals(result);
		boolean bodyKnowsTheOutcome = bodySaysItWasUpdated || bodySaysItWasUnchanged;

		if(bodyKnowsTheOutcome) {
			return false;
		}

		boolean statusIsMissing = false == saveResponse.containsField(CcpJsonCommonsFields.ElasticSearchHttpStatus);

		if(statusIsMissing) {
			return false;
		}

		ElasticSearchHttpStatus status = saveResponse.getAsObject(CcpJsonCommonsFields.ElasticSearchHttpStatus);

		boolean statusSaysItWasInserted = ElasticSearchHttpStatus.CREATED.equals(status);
		return statusSaysItWasInserted;
	}

	private CcpJsonRepresentation retrySave(String entityName, CcpJsonRepresentation json, String id) {
		CcpTimeDecorator ccpTimeDecorator = new CcpTimeDecorator();
		ccpTimeDecorator.sleep(1000);
		CcpJsonRepresentation saveResponse = this.save(entityName, json, id);
		return saveResponse;
	}

	/**
	 * Removes the document and reports whether it existed. Both status {@code 200} and {@code 404} are
	 * treated as valid responses, and the body's {@code result} field tells the two cases apart:
	 * {@code deleted} when the document existed and was removed, {@code not_found} when it never existed.
	 */
	public boolean delete(String entityName, String id) {
		String entityPath = "/" + entityName;
		String docPathPrefix = entityPath + "/_doc/";
		String path = docPathPrefix + id;
		CcpJsonRepresentation handlersFor200 = CcpOtherConstants.EMPTY_JSON.addJsonTransformer(200, CcpOtherConstants.DO_NOTHING);
		CcpJsonRepresentation handlers = handlersFor200.addJsonTransformer(404, CcpOtherConstants.DO_NOTHING);
		CcpDbRequester dbUtils = CcpDependencyInjection.getDependency(CcpDbRequester.class);
		CcpJsonRepresentation response = dbUtils.executeHttpRequest("delete", path, CcpHttpMethods.DELETE, handlers, CcpOtherConstants.EMPTY_JSON, CcpHttpResponseType.singleRecord);
		String result = response.getAsString(CcpJsonCommonsFields.result);
		boolean found = "deleted".equals(result);
		return found;
	}
	
	public CcpSelectUnionAll unionAll(Collection<CcpJsonRepresentation> values, CcpEntity... entities) {
		CcpJsonRepresentation requestBody = this.getRequestBodyToMultipleGet(values, entities);
		int valuesSize = values.size();
		CcpJsonRepresentation[] searchParameters = values.toArray(new CcpJsonRepresentation[valuesSize]);
		CcpSelectUnionAll ccpSelectUnionAll = this.unionAll(requestBody, searchParameters, entities);
		return ccpSelectUnionAll;   
	}


	private CcpSelectUnionAll unionAll(CcpJsonRepresentation requestBody, CcpJsonRepresentation[] searchParameters, CcpEntity... entities) {
		CcpDbRequester dbUtils = CcpDependencyInjection.getDependency(CcpDbRequester.class);
		CcpJsonRepresentation response = dbUtils.executeHttpRequest("getResponseToMultipleGet", "/_mget", CcpHttpMethods.POST, 200, requestBody, CcpHttpResponseType.singleRecord);
		List<CcpJsonRepresentation> docs = response.getAsJsonList(JsonFieldNames.docs);
		Stream<CcpJsonRepresentation> docsStream = docs.stream();
		var sourcesStream = docsStream.map(FunctionResponseHandlerToMget.INSTANCE);
		List<CcpJsonRepresentation> records = sourcesStream.collect(Collectors.toList());
		CcpSelectUnionAll ccpSelectUnionAll = new CcpSelectUnionAll(searchParameters, records, entities);
		return ccpSelectUnionAll;
	}

	public CcpUnionAllExecutor getUnionAllExecutor() {
		return this;
	}
}
