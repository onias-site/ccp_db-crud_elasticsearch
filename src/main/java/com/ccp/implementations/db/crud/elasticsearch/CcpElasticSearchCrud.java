package com.ccp.implementations.db.crud.elasticsearch;

import com.ccp.dependency.injection.CcpInstanceProvider;
import com.ccp.especifications.db.crud.CcpCrud;

/**
 * Provedor de DI que expõe {@code ElasticSearchCrud} como implementação de {@code CcpCrud}.
 */
public class CcpElasticSearchCrud implements CcpInstanceProvider<CcpCrud>  {

	
	public CcpCrud getInstance() {
		return new ElasticSearchCrud();
	}

}
