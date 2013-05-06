package ayizan.service;

import ayizan.domain.Executions.AcceptExecutionOrBuilder;
import ayizan.domain.Executions.CancelExecutionOrBuilder;
import ayizan.domain.Executions.ReplaceExecutionOrBuilder;
import ayizan.domain.Executions.StatusExecutionOrBuilder;
import ayizan.domain.Executions.TradeExecutionOrBuilder;
import ayizan.domain.Orders.CancelOrderSpecificationOrBuilder;
import ayizan.domain.Orders.CancelReplaceOrderSpecificationOrBuilder;
import ayizan.domain.Orders.OrderStatusSpecificationOrBuilder;
import ayizan.domain.Orders.PlaceOrderSpecificationOrBuilder;

public interface ExecutionVenue
{
    public interface ExecutionCallback
    {
        void start();

        void notify(AcceptExecutionOrBuilder acceptExecution);

        void notify(TradeExecutionOrBuilder tradeExecution);

        void notify(ReplaceExecutionOrBuilder replaceExecution);

        void notify(CancelExecutionOrBuilder cancelExecution);

        void notify(StatusExecutionOrBuilder statusExecution);

        void commit();
    }

    void placeOrder(PlaceOrderSpecificationOrBuilder placeOrderSpecification, ExecutionCallback executionCallback);

    void cancelReplaceOrder(CancelReplaceOrderSpecificationOrBuilder cancelReplaceOrderSpecification, ExecutionCallback executionCallback);

    void cancelOrder(CancelOrderSpecificationOrBuilder cancelOrderSpecification, ExecutionCallback executionCallback);

    void orderStatus(OrderStatusSpecificationOrBuilder orderStatusSpecification, ExecutionCallback executionCallback);
}
