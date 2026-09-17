
package com.ccp.implementations.db.crud.elasticsearch;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpErrorCrudMultiGetSearchFailed;
import com.ccp.business.CcpBusiness;


import com.ccp.json.fields.validation.CcpJsonCommonsFields;

/**
 * Singleton {@code CcpBusiness} que processa cada documento retornado pelo {@code _mget}.
 * Extrai o campo {@code _source} e re-adiciona {@code _id} e {@code _index} ao JSON resultante.
 * Lança {@code CcpErrorCrudMultiGetSearchFailed} se o documento contiver campo {@code error}.
 */
class FunctionResponseHandlerToMget implements CcpBusiness{
	
	static final FunctionResponseHandlerToMget INSTANCE = new FunctionResponseHandlerToMget();
	
	private FunctionResponseHandlerToMget() {}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		
		CcpJsonRepresentation error = json.getInnerJson(CcpJsonCommonsFields.error);
		boolean errorEmpty = error.isEmpty();

		boolean hasError = false == errorEmpty;
		
		if(hasError) {
			CcpErrorCrudMultiGetSearchFailed ccpErrorCrudMultiGetSearchFailed = new CcpErrorCrudMultiGetSearchFailed(error);
			throw ccpErrorCrudMultiGetSearchFailed;
		}

		CcpJsonRepresentation internalMap = json.getInnerJson(CcpJsonCommonsFields._source);
		
		String _index = json.getAsString(CcpJsonCommonsFields._index);
		String id = json.getAsString(CcpJsonCommonsFields._id);
		CcpJsonRepresentation put2 = internalMap
				.put(CcpJsonCommonsFields._id, id);

				CcpJsonRepresentation put = put2
				.put(CcpJsonCommonsFields._index, _index)
				;
		
		return put;
	}
	
}