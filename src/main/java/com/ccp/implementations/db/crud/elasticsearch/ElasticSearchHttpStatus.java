package com.ccp.implementations.db.crud.elasticsearch;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;
import com.ccp.json.fields.validation.CcpJsonCommonsFields;

/**
 * Enum que representa os status HTTP relevantes para operações no Elasticsearch.
 * Cada constante implementa {@code CcpBusiness} para registrar o próprio status no JSON de resposta.
 */
enum ElasticSearchHttpStatus implements CcpBusiness{
	OK,
	NOT_FOUND, 
	CREATED;


	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation put = json.addJsonTransformer(CcpJsonCommonsFields.ElasticSearchHttpStatus, this);
		return put;
	}
	
}

