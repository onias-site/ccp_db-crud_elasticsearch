package com.ccp.implementations.db.crud.elasticsearch;

import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
enum ElasticSearchHttpStatus implements  Function<CcpJsonRepresentation, CcpJsonRepresentation>{
	OK,
	NOT_FOUND, 
	CREATED;

	enum JsonFieldNames implements CcpJsonFieldName{
		ElasticSearchHttpStatus
	}

	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
//		ATTENTION se esssa classe "ElasticSearchHttpStatus" mudar de nome haverá uma quebra
		CcpJsonRepresentation put = json.addJsonTransformer(JsonFieldNames.ElasticSearchHttpStatus, this);
		return put;
	}
	
}

