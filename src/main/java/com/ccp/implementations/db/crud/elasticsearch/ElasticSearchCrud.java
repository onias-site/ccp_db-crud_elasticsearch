package com.ccp.implementations.db.crud.elasticsearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.decorators.CcpTimeDecorator;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpErrorDbCrudMultiGetSearchUnfeasible;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.especifications.db.crud.CcpUnionAllExecutor;
import com.ccp.especifications.db.utils.CcpDbRequester;
import com.ccp.especifications.db.utils.CcpEntity;
import com.ccp.especifications.db.utils.CcpErrorBulkEntityRecordNotFound;
import com.ccp.especifications.http.CcpHttpMethods;
import com.ccp.especifications.http.CcpHttpResponseType;
import com.ccp.process.CcpFunctionThrowException;
class ElasticSearchCrud implements CcpCrud, CcpUnionAllExecutor {
	enum JsonFieldNames implements CcpJsonFieldName{
		upsert, params, source, script, lang, painless, _id, _index, docs, result, ElasticSearchHttpStatus
	}

	private CcpJsonRepresentation getRequestBodyToMultipleGet(Collection<CcpJsonRepresentation> jsons, CcpEntity... entities) {
		
		List<CcpJsonRepresentation> docs = new ArrayList<CcpJsonRepresentation>();
		
		for (CcpEntity entity : entities) {
			
			for (CcpJsonRepresentation json : jsons) {
				
 				List<String> primaryKeyNames = entity.getPrimaryKeyNames();
				
				boolean anyKeyIsMissing = false == json.containsAllFields(primaryKeyNames);
				
				if(anyKeyIsMissing) {
					continue;
				}

				List<CcpJsonRepresentation> parametersToSearch = entity.getParametersToSearch(json);
				docs.addAll(parametersToSearch);
			}
		}
		
		boolean unfeasibleMultiGetSearch = docs.isEmpty();
	
		if(unfeasibleMultiGetSearch) {
			throw new CcpErrorDbCrudMultiGetSearchUnfeasible(jsons, entities);
		}
		
		CcpJsonRepresentation requestBody = CcpOtherConstants.EMPTY_JSON.put(JsonFieldNames.docs, docs);
		return requestBody;
	}
	
	public CcpJsonRepresentation getRequestBodyToMultipleGet(Set<String> ids, CcpEntity... entities) {
		List<CcpJsonRepresentation> docs1 = new ArrayList<CcpJsonRepresentation>();
		for (CcpEntity entity : entities) {
			String entidade = entity.getEntityName();
			for (String id : ids) {
				CcpJsonRepresentation put = CcpOtherConstants.EMPTY_JSON
				.put(JsonFieldNames._index, entidade)
				.put(JsonFieldNames._id, id)
				;
				docs1.add(put);
			}
		}
		CcpJsonRepresentation requestBody = CcpOtherConstants.EMPTY_JSON.put(JsonFieldNames.docs, docs1);
		return requestBody;
	}
	
	public CcpJsonRepresentation getOneById(String entityName, String id) {
		String path = "/" + entityName + "/_source/" + id ;
		CcpJsonRepresentation handlers = CcpOtherConstants.EMPTY_JSON.addJsonTransformer(200, CcpOtherConstants.DO_NOTHING).addJsonTransformer(404, new CcpFunctionThrowException(new CcpErrorBulkEntityRecordNotFound(entityName, id)));
		
		CcpDbRequester dbUtils = CcpDependencyInjection.getDependency(CcpDbRequester.class);
		CcpJsonRepresentation response = dbUtils.executeHttpRequest("getOneById", path, CcpHttpMethods.GET, handlers, CcpOtherConstants.EMPTY_JSON, CcpHttpResponseType.singleRecord);
		
		return response;
	}

	public boolean exists(String entityName, String id) {
		String path = "/" + entityName + "/_doc/" + id;
		
		CcpJsonRepresentation flows = CcpOtherConstants.EMPTY_JSON.addJsonTransformer(200, ElasticSearchHttpStatus.OK)
				.addJsonTransformer(404,  ElasticSearchHttpStatus.NOT_FOUND);
		
		CcpDbRequester dbUtils = CcpDependencyInjection.getDependency(CcpDbRequester.class);
		CcpJsonRepresentation response = dbUtils.executeHttpRequest("exists", path, CcpHttpMethods.HEAD, flows, CcpOtherConstants.EMPTY_JSON, CcpHttpResponseType.singleRecord);
//		ATTENTION se esssa classe "ElasticSearchHttpStatus" mudar de nome haverá uma quebra
		ElasticSearchHttpStatus status = response.getAsObject(JsonFieldNames.ElasticSearchHttpStatus);
		
		boolean exists = ElasticSearchHttpStatus.OK.equals(status);
		return exists;
	}

	public CcpJsonRepresentation save(String entityName, CcpJsonRepresentation json, String id) {
		String path = "/" + entityName + "/_update/" + id;
		
		CcpJsonRepresentation requestBody = CcpOtherConstants.EMPTY_JSON
				.addToItem(JsonFieldNames.script, JsonFieldNames.lang, JsonFieldNames.painless.name())
				.addToItem(JsonFieldNames.script, JsonFieldNames.source, "ctx._source.putAll(params);")
				.addToItem(JsonFieldNames.script, JsonFieldNames.params, json)
				.put(JsonFieldNames.upsert, json)
				;
		
		CcpJsonRepresentation handlers = CcpOtherConstants.EMPTY_JSON
				.addJsonTransformer(409, values -> this.retrySave(entityName, json, id))
				.addJsonTransformer(201,  ElasticSearchHttpStatus.CREATED)
				.addJsonTransformer(200, ElasticSearchHttpStatus.OK)
				;
		
		CcpDbRequester dbUtils = CcpDependencyInjection.getDependency(CcpDbRequester.class);
		CcpJsonRepresentation response = dbUtils.executeHttpRequest("createOrUpdate", path, CcpHttpMethods.POST, handlers, requestBody, CcpHttpResponseType.singleRecord);
		return response;
	}

	private CcpJsonRepresentation retrySave(String entityName, CcpJsonRepresentation json, String id) {
		new CcpTimeDecorator().sleep(1000);
		CcpJsonRepresentation createOrUpdate = this.save(entityName, json, id);
		return createOrUpdate;
	}

	public boolean delete(String entityName, String id) {
		String path = "/" + entityName + "/_doc/" + id;
		CcpJsonRepresentation handlers = CcpOtherConstants.EMPTY_JSON.addJsonTransformer(200, CcpOtherConstants.DO_NOTHING).addJsonTransformer(404, CcpOtherConstants.DO_NOTHING);
		CcpDbRequester dbUtils = CcpDependencyInjection.getDependency(CcpDbRequester.class);
		CcpJsonRepresentation response = dbUtils.executeHttpRequest("delete", path, CcpHttpMethods.DELETE, handlers, CcpOtherConstants.EMPTY_JSON, CcpHttpResponseType.singleRecord);
		String result = response.getAsString(JsonFieldNames.result);
		boolean found = "deleted".equals(result);
		return found;
	}
	
	public CcpSelectUnionAll unionAll(Collection<CcpJsonRepresentation> values, CcpEntity... entities) {
		CcpJsonRepresentation requestBody = this.getRequestBodyToMultipleGet(values, entities);
		CcpSelectUnionAll ccpSelectUnionAll = this.unionAll(requestBody);
		return ccpSelectUnionAll;
	}

	private CcpSelectUnionAll unionAll(CcpJsonRepresentation requestBody) {
		CcpDbRequester dbUtils = CcpDependencyInjection.getDependency(CcpDbRequester.class);
		CcpJsonRepresentation response = dbUtils.executeHttpRequest("getResponseToMultipleGet", "/_mget", CcpHttpMethods.POST, 200, requestBody, CcpHttpResponseType.singleRecord);
		List<CcpJsonRepresentation> docs = response.getAsJsonList(JsonFieldNames.docs);
		List<CcpJsonRepresentation> asMapList = docs.stream().map(FunctionResponseHandlerToMget.INSTANCE).collect(Collectors.toList());
		CcpSelectUnionAll ccpSelectUnionAll = new CcpSelectUnionAll(asMapList);
		return ccpSelectUnionAll;
	}

	public CcpUnionAllExecutor getUnionAllExecutor() {
		return this;
	}
}
