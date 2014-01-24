package com.eviware.soapui.impl.rest.mock;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.config.*;
import com.eviware.soapui.impl.rest.RestResource;
import com.eviware.soapui.impl.support.AbstractMockOperation;
import com.eviware.soapui.impl.wsdl.mock.DispatchException;
import com.eviware.soapui.model.iface.Interface;
import com.eviware.soapui.model.iface.Operation;
import com.eviware.soapui.support.StringUtils;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;

public class RestMockAction extends AbstractMockOperation<RESTMockActionConfig, RestMockResponse>
{
	private RestResource resource = null;
	private List<RestMockResponse> responses = new ArrayList<RestMockResponse>();

	public RestMockAction( RestMockService mockService, RESTMockActionConfig config )
	{
		super( config, mockService, RestMockAction.getIconName( config ) );

		Interface iface = mockService.getProject().getInterfaceByName( mockService.getName() );
		if( iface == null )
		{
			SoapUI.log.warn( "Missing interface [" + mockService.getName() + "] for MockOperation in project" );
		}
		else
		{
			resource = ( RestResource )iface.getOperationByName( mockService.getName() );
		}

		List<RESTMockResponseConfig> responseConfigs = config.getResponseList();
		for( RESTMockResponseConfig responseConfig : responseConfigs )
		{
			RestMockResponse restMockResponse = new RestMockResponse( this, responseConfig );
			restMockResponse.addPropertyChangeListener( this );
			responses.add( restMockResponse );
		}

		super.setupConfig(config);
	}

	public static String getIconName(RESTMockActionConfig methodConfig)
	{
		String method = StringUtils.isNullOrEmpty( methodConfig.getMethod() ) ? "get" : methodConfig.getMethod().toLowerCase();
		return "/" + method + "_method.gif";
	}

	@Override
	public RestMockService getMockService()
	{
		return ( RestMockService )getParent();
	}

	@Override
	public void removeResponseFromConfig( int index )
	{
		getConfig().removeResponse( index );
	}

	@Override
	public Operation getOperation()
	{
		return resource;
	}

	@Override
	public void propertyChange( PropertyChangeEvent evt )
	{

	}

	public RestMockResponse addNewMockResponse( RESTMockResponseConfig responseConfig )
	{
		RestMockResponse mockResponse = new RestMockResponse( this, responseConfig );

		responses.add( mockResponse );

		if( getMockResponseCount() == 1 )
		{
			setDefaultResponse( responseConfig.getResponseContent().toString() );
		}

		( getMockService() ).fireMockResponseAdded( mockResponse );
		notifyPropertyChanged( "mockResponses", null, mockResponse );

		return mockResponse;
	}

	public RestMockResult dispatchRequest( RestMockRequest request ) throws DispatchException
	{
		try
		{
			RestMockResult result = new RestMockResult( request );

			if( getMockResponseCount() == 0 )
				throw new DispatchException( "Missing MockResponse(s) in MockOperation [" + getName() + "]" );

			result.setMockOperation( this );
			RestMockResponse response = responses.get( 0 ); // TODO in SOAP-1334

			if( response == null )
			{
				// TODO in SOAP-1334 - when there is no matchin response from the dispatcher strategy - use the default
				throw new UnknownError( "not implemented" );
			}

			if( response == null )
			{
				throw new DispatchException( "Failed to find MockResponse" );
			}

			result.setMockResponse( response );
			response.execute( request, result );

			return result;
		}
		catch( Throwable e )
		{
			if( e instanceof DispatchException )
				throw ( DispatchException )e;
			else
				throw new DispatchException( e );
		}
	}
}