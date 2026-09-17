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
 * Implementação principal de {@code CcpCrud} e {@code CcpUnionAllExecutor} para o Elasticsearch.
 * Oferece operações de leitura ({@code getOneById}, {@code exists}, {@code unionAll}),
 * escrita ({@code save} com upsert via script Painless) e remoção ({@code delete}).
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
		List<CcpJsonRepresentation> docs1 = new ArrayList<CcpJsonRepresentation>();
		for (CcpEntity entity : entities) {
			CcpEntityMetaData entityDetails = entity.getEntityMetaData();
			for (String id : ids) {
				CcpJsonRepresentation put2 = CcpOtherConstants.EMPTY_JSON
				.put(CcpJsonCommonsFields._index, entityDetails.entityName);
				CcpJsonRepresentation put = put2
				.put(CcpJsonCommonsFields._id, id)
				;
				docs1.add(put);
			}
		}
		CcpJsonRepresentation requestBody = CcpOtherConstants.EMPTY_JSON.put(JsonFieldNames.docs, docs1);
		return requestBody;
	}
	
	public CcpJsonRepresentation getOneById(String entityName, String id) {
		String valorMais = "/" + entityName;
		String valorMaisMais = valorMais + "/_source/";
		String path = valorMaisMais + id ;
		CcpJsonRepresentation addJsonTransformer = CcpOtherConstants.EMPTY_JSON.addJsonTransformer(200, CcpOtherConstants.DO_NOTHING);
		CcpErrorBulkEntityRecordNotFound ccpErrorBulkEntityRecordNotFound = new CcpErrorBulkEntityRecordNotFound(entityName, id);
		CcpFunctionThrowException ccpFunctionThrowException = new CcpFunctionThrowException(ccpErrorBulkEntityRecordNotFound);
		CcpJsonRepresentation handlers = addJsonTransformer.addJsonTransformer(404, ccpFunctionThrowException);
		
		CcpDbRequester dbUtils = CcpDependencyInjection.getDependency(CcpDbRequester.class);
		CcpJsonRepresentation response = dbUtils.executeHttpRequest("getOneById", path, CcpHttpMethods.GET, handlers, CcpOtherConstants.EMPTY_JSON, CcpHttpResponseType.singleRecord);
		
		return response;
	}

	public boolean exists(String entityName, String id) {
		String valorMais2 = "/" + entityName;
		String valorMais2Mais = valorMais2 + "/_doc/";
		String path = valorMais2Mais + id;
		CcpJsonRepresentation addJsonTransformer2 = CcpOtherConstants.EMPTY_JSON.addJsonTransformer(200, ElasticSearchHttpStatus.OK);
	
		CcpJsonRepresentation flows = addJsonTransformer2
				.addJsonTransformer(404,  ElasticSearchHttpStatus.NOT_FOUND);
		
		CcpDbRequester dbUtils = CcpDependencyInjection.getDependency(CcpDbRequester.class);
		CcpJsonRepresentation response = dbUtils.executeHttpRequest("exists", path, CcpHttpMethods.HEAD, flows, CcpOtherConstants.EMPTY_JSON, CcpHttpResponseType.singleRecord);
		ElasticSearchHttpStatus status = response.getAsObject(CcpJsonCommonsFields.ElasticSearchHttpStatus);
		
		boolean exists = ElasticSearchHttpStatus.OK.equals(status);
		return exists;
	}

	public CcpJsonRepresentation save(String entityName, CcpJsonRepresentation json, String id) {
		String valorMais3 = "/" + entityName;
		String valorMais3Mais = valorMais3 + "/_update/";
		String path = valorMais3Mais + id;
		String painlessName = JsonFieldNames.painless.name();
		CcpJsonRepresentation addToItem = CcpOtherConstants.EMPTY_JSON
				.addToItem(JsonFieldNames.script, JsonFieldNames.lang, painlessName);
				CcpJsonRepresentation addToItem2 = addToItem
				.addToItem(JsonFieldNames.script, JsonFieldNames.source, "ctx._source.putAll(params);");
				CcpJsonRepresentation addToItem3 = addToItem2
				.addToItem(JsonFieldNames.script, JsonFieldNames.params, json);

				CcpJsonRepresentation requestBody = addToItem3
				.put(JsonFieldNames.upsert, json)
				;
				CcpJsonRepresentation addJsonTransformer3 = CcpOtherConstants.EMPTY_JSON
				.addJsonTransformer(409, values -> this.retrySave(entityName, json, id));
				CcpJsonRepresentation addJsonTransformer4 = addJsonTransformer3
				.addJsonTransformer(201,  ElasticSearchHttpStatus.CREATED);

				CcpJsonRepresentation handlers = addJsonTransformer4
				.addJsonTransformer(200, ElasticSearchHttpStatus.OK)
				;
		
		CcpDbRequester dbUtils = CcpDependencyInjection.getDependency(CcpDbRequester.class);
		CcpJsonRepresentation response = dbUtils.executeHttpRequest("createOrUpdate", path, CcpHttpMethods.POST, handlers, requestBody, CcpHttpResponseType.singleRecord);
		return response;
	}

	/**
	 * Interpreta a resposta do {@code _update} do Elasticsearch para distinguir uma inclusão de uma
	 * atualização. São consultados os dois sinais que a resposta carrega, nesta ordem:
	 * <ul>
	 * <li>o campo {@code result} do corpo, que traz o desfecho no nível do documento: {@code created}
	 * quando o {@code upsert} inseriu o documento, {@code updated} quando o script alterou um documento
	 * já existente e {@code noop} quando o documento já estava com o conteúdo enviado;</li>
	 * <li>o status HTTP, registrado no json pelos handlers de {@code save} ({@code 201} vira
	 * {@code CREATED} na inclusão e {@code 200} vira {@code OK} na atualização), usado quando o corpo
	 * não trouxe o desfecho. Qualquer outro status não mapeado nem chega aqui, pois o
	 * {@code CcpHttpHandler} lança {@code CcpErrorHttp} antes.</li>
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
		CcpJsonRepresentation createOrUpdate = this.save(entityName, json, id);
		return createOrUpdate;
	}

	/**
	 * Remove o documento e informa se ele existia. Tanto o status {@code 200} quanto o {@code 404} são
	 * tratados como respostas válidas, e é o campo {@code result} do corpo que distingue os dois casos:
	 * {@code deleted} quando o documento existia e foi removido e {@code not_found} quando ele nem existia.
	 */
	public boolean delete(String entityName, String id) {
		String valorMais4 = "/" + entityName;
		String valorMais4Mais = valorMais4 + "/_doc/";
		String path = valorMais4Mais + id;
		CcpJsonRepresentation addJsonTransformer5 = CcpOtherConstants.EMPTY_JSON.addJsonTransformer(200, CcpOtherConstants.DO_NOTHING);
		CcpJsonRepresentation handlers = addJsonTransformer5.addJsonTransformer(404, CcpOtherConstants.DO_NOTHING);
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
		Stream<CcpJsonRepresentation> stream = docs.stream();
		var streamMap = stream.map(FunctionResponseHandlerToMget.INSTANCE);
		List<CcpJsonRepresentation> asMapList = streamMap.collect(Collectors.toList());
		CcpSelectUnionAll ccpSelectUnionAll = new CcpSelectUnionAll(searchParameters, asMapList, entities);
		return ccpSelectUnionAll;
	}

	public CcpUnionAllExecutor getUnionAllExecutor() {
		return this;
	}
}
