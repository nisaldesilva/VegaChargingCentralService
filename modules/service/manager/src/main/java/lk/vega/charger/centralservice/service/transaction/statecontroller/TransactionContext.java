package lk.vega.charger.centralservice.service.transaction.statecontroller;

import lk.vega.charger.centralservice.service.AuthorizeRequest;
import lk.vega.charger.centralservice.service.StartTransactionRequest;
import lk.vega.charger.centralservice.service.StopTransactionRequest;
import lk.vega.charger.core.ChargeTransaction;
import lk.vega.charger.util.ChgResponse;

/**
 * Created by Dileepa on 3/21/15.
 */
public class TransactionContext
{
    private TransactionState transactionState;
    private ChargeTransaction chargeTransaction;
    private AuthorizeRequest authorizeRequest;
    private StartTransactionRequest startTransactionRequest;
    private StopTransactionRequest stopTransactionRequest;

    public StopTransactionRequest getStopTransactionRequest()
    {
        return stopTransactionRequest;
    }

    public void setStopTransactionRequest( StopTransactionRequest stopTransactionRequest )
    {
        this.stopTransactionRequest = stopTransactionRequest;
    }

    public StartTransactionRequest getStartTransactionRequest()
    {
        return startTransactionRequest;
    }

    public void setStartTransactionRequest(StartTransactionRequest startTransactionRequest)
    {
        this.startTransactionRequest = startTransactionRequest;
    }

    public AuthorizeRequest getAuthorizeRequest()
    {
        return authorizeRequest;
    }

    public void setAuthorizeRequest(AuthorizeRequest authorizeRequest)
    {
        this.authorizeRequest = authorizeRequest;
    }
    public ChargeTransaction getChargeTransaction()
    {
        return chargeTransaction;
    }

    public void setChargeTransaction(ChargeTransaction chargeTransaction)
    {
        this.chargeTransaction = chargeTransaction;
    }
    public TransactionState getTransactionState()
    {
        return transactionState;
    }

    public void setTransactionState(TransactionState transactionState)
    {
        this.transactionState = transactionState;
    }


    public ChgResponse proceedState()
    {
        ChgResponse chgResponse = this.transactionState.proceedState(this);
        return chgResponse;
    }
}
