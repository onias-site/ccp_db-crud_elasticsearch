package com.ccp.implementations.db.crud.elasticsearch;

import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
enum ElasticSearchHttpStatusConstants{
	ElasticSearchHttpStatus
}
enum ElasticSearchHttpStatus implements  Function<CcpJsonRepresentation, CcpJsonRepresentation>{
	OK,
	NOT_FOUND, 
	CREATED;


	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
//		FIXME se esssa classe "ElasticSearchHttpStatus" mudar de nome haverá uma quebra
		CcpJsonRepresentation put = json.addJsonTransformer(ElasticSearchHttpStatusConstants.ElasticSearchHttpStatus, this);
		return put;
	}
	
}

