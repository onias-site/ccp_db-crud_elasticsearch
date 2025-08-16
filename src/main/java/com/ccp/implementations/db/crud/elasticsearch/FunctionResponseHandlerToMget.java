
package com.ccp.implementations.db.crud.elasticsearch;

import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.exceptions.db.crud.CcpErrorDbCrudMultiGetSearchFailed;

enum FunctionResponseHandlerToMgetConstants  implements CcpJsonFieldName{
	error, _source, _index, _id
	
}

class FunctionResponseHandlerToMget implements Function<CcpJsonRepresentation, CcpJsonRepresentation>{
	
	static FunctionResponseHandlerToMget INSTANCE = new FunctionResponseHandlerToMget();
	
	private FunctionResponseHandlerToMget() {}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		
		CcpJsonRepresentation error = json.getInnerJson(FunctionResponseHandlerToMgetConstants.error);
		
		boolean hasError = error.isEmpty() == false;
		
		if(hasError) {
			throw new CcpErrorDbCrudMultiGetSearchFailed(error);
		}

		CcpJsonRepresentation internalMap = json.getInnerJson(FunctionResponseHandlerToMgetConstants._source);
		
		String _index = json.getAsString(FunctionResponseHandlerToMgetConstants._index);
		String id = json.getAsString(FunctionResponseHandlerToMgetConstants._id);

		CcpJsonRepresentation put = internalMap
				.put(FunctionResponseHandlerToMgetConstants._id, id)
				.put(FunctionResponseHandlerToMgetConstants._index, _index)
				;
		
		return put;
	}
	
}