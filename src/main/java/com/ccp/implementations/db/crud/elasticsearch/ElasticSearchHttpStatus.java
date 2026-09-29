package com.ccp.implementations.db.crud.elasticsearch;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;
import com.ccp.json.fields.validation.CcpJsonCommonsFields;

/**
 * Enum representing the HTTP statuses relevant to Elasticsearch operations.
 * Each constant implements {@code CcpBusiness} to record its own status in the response JSON.
 */
enum ElasticSearchHttpStatus implements CcpBusiness{
	OK,
	NOT_FOUND,
	CREATED;



	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation jsonWithStatus = json.addJsonTransformer(CcpJsonCommonsFields.ElasticSearchHttpStatus, this);
		return jsonWithStatus;
	}
	
}

