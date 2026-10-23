package com.ccp.implementations.db.crud.elasticsearch;

import com.ccp.dependency.injection.CcpInstanceProvider;
import com.ccp.especifications.db.crud.CcpCrud;

/**
 * DI provider that exposes {@code ElasticSearchCrud} as the {@code CcpCrud} implementation.
 */
public class CcpElasticSearchCrud implements CcpInstanceProvider<CcpCrud>  {
	/** The single instance: the class keeps no state. */
	public final static CcpElasticSearchCrud INSTANCE = new CcpElasticSearchCrud();

	private CcpElasticSearchCrud() {}

	/**
	 * Builds the Elasticsearch implementation of {@code CcpCrud}.
	 * @return a new {@code ElasticSearchCrud}
	 */
	public CcpCrud getInstance() {
		ElasticSearchCrud elasticSearchCrud = ElasticSearchCrud.INSTANCE;
		return elasticSearchCrud;
	}

}
