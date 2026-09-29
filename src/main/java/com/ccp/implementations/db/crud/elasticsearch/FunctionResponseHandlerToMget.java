
package com.ccp.implementations.db.crud.elasticsearch;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpErrorCrudMultiGetSearchFailed;
import com.ccp.business.CcpBusiness;


import com.ccp.json.fields.validation.CcpJsonCommonsFields;

/**
 * Singleton {@code CcpBusiness} that processes each document returned by {@code _mget}.
 * Extracts the {@code _source} field and adds {@code _id} and {@code _index} back to the resulting JSON.
 * Throws {@code CcpErrorCrudMultiGetSearchFailed} if the document contains an {@code error} field.
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

		CcpJsonRepresentation source = json.getInnerJson(CcpJsonCommonsFields._source);

		String _index = json.getAsString(CcpJsonCommonsFields._index);
		String id = json.getAsString(CcpJsonCommonsFields._id);
		CcpJsonRepresentation sourceWithId = source
				.put(CcpJsonCommonsFields._id, id);

				CcpJsonRepresentation sourceWithIdAndIndex = sourceWithId
				.put(CcpJsonCommonsFields._index, _index)
				;

		return sourceWithIdAndIndex;
	}
	
}