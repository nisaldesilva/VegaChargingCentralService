package lk.vega.charger.centralservice.service.Impl;

import lk.vega.charger.centralservice.service.*;
import lk.vega.charger.centralservice.service.transaction.TransactionController;
import lk.vega.charger.centralservice.service.transaction.statecontroller.*;
import lk.vega.charger.core.ChargeTransaction;
import lk.vega.charger.util.ChgResponse;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.jws.WebMethod;
import javax.jws.WebParam;

/**
 * Created with IntelliJ IDEA.
 * User: dileepa
 * Date: 3/18/15
 * Time: 5:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class VegaChargingCentralManager implements CentralSystemService
{

    @WebMethod(exclude = true) @PostConstruct
    public void initJaxWS()
    {
        //TODO load service configurations.
    }

    @WebMethod(exclude = true) @PreDestroy
    public void destroy()
    {

    }

    /**
     * in this request, authorizeKey has particular format
     * phonenum#intialamount@timestamp
     * @param parameters
     * @return id  phonenum#intialamount@timestamp%crossReference
     */

    @Override
    public AuthorizeResponse authorize( @WebParam(name = "authorizeRequest", targetNamespace = "urn://Ocpp/Cs/2012/06/", partName = "parameters") AuthorizeRequest parameters )
    {
        String authorizeKey = parameters.getIdTag();
        ChgResponse chgResponse = TransactionController.loadProcessingTransaction( authorizeKey, TransactionController.TRS_STARTED );

        AuthorizeResponse authorizeResponse = new AuthorizeResponse();
        IdTagInfo idTagInfo = authorizeResponse.getIdTagInfo();
        idTagInfo.setParentIdTag( parameters.getIdTag() );

        if( chgResponse.isSuccess() )
        {
            if( chgResponse.getErrorCode().equals( TransactionController.TRS_NEW ) )
            {
                TransactionContext transactionContext = new TransactionContext();
                transactionContext.setAuthorizeRequest( parameters );
                TransactionState transactionBeginningState = new TransactionBeginningState();
                transactionContext.setTransactionState( transactionBeginningState );
                ChgResponse response = transactionContext.proceedState();
                if( response.isError() )
                {
                    idTagInfo.setStatus( AuthorizationStatus.BLOCKED );
                }
                else if( response.isSuccess() )
                {
                    String paymentGatewayCrossRef = (String) response.getReturnData();
                    idTagInfo.setStatus( AuthorizationStatus.ACCEPTED );
                    StringBuilder sb = new StringBuilder();
                    sb.append( parameters.getIdTag() );
                    sb.append( TransactionController.TRS_CROSS_REF_SPLITTER );//use for separate the cross reference of payment gate way.
                    sb.append( paymentGatewayCrossRef );
                    idTagInfo.setParentIdTag( sb.toString() );
                }
            }
            else
            {
                ChargeTransaction inProgressChargeTransaction = (ChargeTransaction) chgResponse.getReturnData();
                TransactionContext transactionContext = new TransactionContext();
                transactionContext.setAuthorizeRequest( parameters );
                transactionContext.setChargeTransaction( inProgressChargeTransaction );
                TransactionState transactionStartedState = new TransactionProceedState();
                transactionContext.setTransactionState( transactionStartedState );
                ChgResponse res = transactionContext.proceedState();
                if( res.isError() )
                {
                    idTagInfo.setStatus( AuthorizationStatus.BLOCKED );
                }
                else if( res.isSuccess() )
                {
                    idTagInfo.setStatus( AuthorizationStatus.ACCEPTED );
                    StringBuilder sb = new StringBuilder();
                    sb.append( inProgressChargeTransaction.getAuthenticationKey() );
                    sb.append( TransactionController.TRS_CROSS_REF_SPLITTER );//use for separate the cross reference of payment gate way.
                    sb.append( inProgressChargeTransaction.getCrossReference() );
                    idTagInfo.setParentIdTag( sb.toString() );
                }
            }
        }
        else if( chgResponse.isError() )
        {
            idTagInfo.setStatus( AuthorizationStatus.BLOCKED );
        }
        return authorizeResponse;
    }

    @Override
    public StartTransactionResponse startTransaction( @WebParam(name = "startTransactionRequest", targetNamespace = "urn://Ocpp/Cs/2012/06/", partName = "parameters") StartTransactionRequest parameters )
    {
        StartTransactionResponse startTransactionResponse = new StartTransactionResponse();
        IdTagInfo idTagInfo = startTransactionResponse.getIdTagInfo();
        idTagInfo.setParentIdTag( parameters.getIdTag() );

        TransactionContext transactionContext = new TransactionContext();
        transactionContext.setStartTransactionRequest(parameters);
        TransactionState transactionStartedState = new TransactionStartedState();
        transactionContext.setTransactionState(transactionStartedState);
        ChgResponse chgResponse = transactionContext.proceedState();
        if (chgResponse.isSuccess())
        {
            ChargeTransaction chargeTransaction = (ChargeTransaction)chgResponse.getReturnData();
            idTagInfo.setStatus( AuthorizationStatus.ACCEPTED );
            startTransactionResponse.setTransactionId( Integer.parseInt( chargeTransaction.getTransactionId() ) );
        }
        else
        {
            idTagInfo.setStatus( AuthorizationStatus.BLOCKED );
            startTransactionResponse.setTransactionId( -1 );
        }
        return startTransactionResponse;
    }

    @Override
    public StopTransactionResponse stopTransaction( @WebParam(name = "stopTransactionRequest", targetNamespace = "urn://Ocpp/Cs/2012/06/", partName = "parameters") StopTransactionRequest parameters )
    {
        StopTransactionResponse stopTransactionResponse = new StopTransactionResponse();
        IdTagInfo idTagInfo = stopTransactionResponse.getIdTagInfo();

        String authorizeKeyWithCrossRef = parameters.getIdTag();
        String []authKeyCrossRefArray = TransactionController.phoneNumAmountAndCrossRefSeparator( authorizeKeyWithCrossRef );
        String authorizeKey = authKeyCrossRefArray[0];
        ChgResponse response = TransactionController.loadProcessingTransaction( authorizeKey, TransactionController.TRS_PROCESSED );
        if (response.isSuccess())
        {
            ChargeTransaction inProgressChargeTransaction = (ChargeTransaction)response.getReturnData();
            if (inProgressChargeTransaction != null)
            {
                TransactionContext transactionContext = new TransactionContext();
                transactionContext.setChargeTransaction(inProgressChargeTransaction);
                transactionContext.setStopTransactionRequest( parameters );
                TransactionState transactionStartedState = new TransactionStoppedState();
                transactionContext.setTransactionState(transactionStartedState);
                ChgResponse chgResponse = transactionContext.proceedState();
                if (chgResponse.isSuccess())
                {
                    idTagInfo.setStatus( AuthorizationStatus.ACCEPTED );
                    idTagInfo.setParentIdTag( parameters.getIdTag() );
                }
                else
                {
                    idTagInfo.setStatus( AuthorizationStatus.BLOCKED );
                    idTagInfo.setParentIdTag( parameters.getIdTag() );
                }
            }
        }
        return stopTransactionResponse;
    }

    @Override
    public HeartbeatResponse heartbeat( @WebParam(name = "heartbeatRequest", targetNamespace = "urn://Ocpp/Cs/2012/06/", partName = "parameters") HeartbeatRequest parameters )
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public MeterValuesResponse meterValues( @WebParam(name = "meterValuesRequest", targetNamespace = "urn://Ocpp/Cs/2012/06/", partName = "parameters") MeterValuesRequest parameters )
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public BootNotificationResponse bootNotification( @WebParam(name = "bootNotificationRequest", targetNamespace = "urn://Ocpp/Cs/2012/06/", partName = "parameters") BootNotificationRequest parameters )
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public StatusNotificationResponse statusNotification( @WebParam(name = "statusNotificationRequest", targetNamespace = "urn://Ocpp/Cs/2012/06/", partName = "parameters") StatusNotificationRequest parameters )
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public FirmwareStatusNotificationResponse firmwareStatusNotification( @WebParam(name = "firmwareStatusNotificationRequest", targetNamespace = "urn://Ocpp/Cs/2012/06/", partName = "parameters") FirmwareStatusNotificationRequest parameters )
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public DiagnosticsStatusNotificationResponse diagnosticsStatusNotification( @WebParam(name = "diagnosticsStatusNotificationRequest", targetNamespace = "urn://Ocpp/Cs/2012/06/", partName = "parameters") DiagnosticsStatusNotificationRequest parameters )
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public DataTransferResponse dataTransfer( @WebParam(name = "dataTransferRequest", targetNamespace = "urn://Ocpp/Cs/2012/06/", partName = "parameters") DataTransferRequest parameters )
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
