
package com.ccp.implementations.db.crud.elasticsearch;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.especifications.db.crud.CcpErrorCrudMultiGetSearchFailed;
import com.ccp.business.CcpBusiness;


class FunctionResponseHandlerToMget implements CcpBusiness{
	enum JsonFieldNames implements CcpJsonFieldName{
		error, _source, _index, _id
	}
	
	static FunctionResponseHandlerToMget INSTANCE = new FunctionResponseHandlerToMget();
	
	private FunctionResponseHandlerToMget() {}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		
		CcpJsonRepresentation error = json.getInnerJson(JsonFieldNames.error);
		
		boolean hasError = false == error.isEmpty();
		
		if(hasError) {
			throw new CcpErrorCrudMultiGetSearchFailed(error);
		}

		CcpJsonRepresentation internalMap = json.getInnerJson(JsonFieldNames._source);
		
		String _index = json.getAsString(JsonFieldNames._index);
		String id = json.getAsString(JsonFieldNames._id);

		CcpJsonRepresentation put = internalMap
				.put(JsonFieldNames._id, id)
				.put(JsonFieldNames._index, _index)
				;
		
		return put;
	}
	
}