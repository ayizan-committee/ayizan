package ayizan.service;

import ayizan.domain.Orders.CancelOrderSpecification;
import ayizan.domain.Orders.CancelReplaceOrderSpecification;
import ayizan.domain.Orders.Execution;
import ayizan.domain.Orders.OrderSpecification;
import ayizan.domain.Orders.OrderStatusSpecification;

public interface ExecutionVenue
{
    public interface ExecutionCallback
    {
        void notify(Execution execution);
    }

    void placeOrder(OrderSpecification orderSpecification, ExecutionCallback executionCallback);

    void cancelReplaceOrder(CancelReplaceOrderSpecification cancelReplaceOrderSpecification, ExecutionCallback executionCallback);

    void cancelOrder(CancelOrderSpecification cancelOrderSpecification, ExecutionCallback executionCallback);

    void orderStatus(OrderStatusSpecification orderStatusSpecification, ExecutionCallback executionCallback);
}
