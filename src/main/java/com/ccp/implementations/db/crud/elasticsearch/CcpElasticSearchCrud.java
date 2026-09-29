package com.ccp.implementations.db.crud.elasticsearch;

import com.ccp.dependency.injection.CcpInstanceProvider;
import com.ccp.especifications.db.crud.CcpCrud;

/**
 * DI provider that exposes {@code ElasticSearchCrud} as the {@code CcpCrud} implementation.
 */
public class CcpElasticSearchCrud implements CcpInstanceProvider<CcpCrud>  {


	public CcpCrud getInstance() {
		ElasticSearchCrud elasticSearchCrud = new ElasticSearchCrud();
		return elasticSearchCrud;
	}

}
