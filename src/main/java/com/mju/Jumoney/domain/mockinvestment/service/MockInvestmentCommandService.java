package com.mju.Jumoney.domain.mockinvestment.service;

import com.mju.Jumoney.domain.mockinvestment.domain.Account;
import com.mju.Jumoney.domain.mockinvestment.domain.Order;
import com.mju.Jumoney.domain.mockinvestment.domain.Portfolio;
import com.mju.Jumoney.domain.mockinvestment.dto.MockInvestmentOrderRequest;
import com.mju.Jumoney.domain.mockinvestment.dto.MockInvestmentOrderResponse;
import com.mju.Jumoney.domain.mockinvestment.enums.OrderType;
import com.mju.Jumoney.domain.mockinvestment.exception.MockInvestmentErrorCode;
import com.mju.Jumoney.domain.mockinvestment.repository.OrderRepository;
import com.mju.Jumoney.domain.mockinvestment.repository.PortfolioRepository;
import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.domain.stock.dto.StockCurrentPriceSnapshot;
import com.mju.Jumoney.domain.stock.exception.StockErrorCode;
import com.mju.Jumoney.domain.stock.repository.StockRepository;
import com.mju.Jumoney.domain.stock.service.StockCurrentPriceService;
import com.mju.Jumoney.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class MockInvestmentCommandService {

    private final MockInvestmentAccountService mockInvestmentAccountService;
    private final StockRepository stockRepository;
    private final PortfolioRepository portfolioRepository;
    private final OrderRepository orderRepository;
    private final StockCurrentPriceService stockCurrentPriceService;
    private final MockInvestmentMarketService mockInvestmentMarketService;

    public MockInvestmentOrderResponse buy(Long userId, MockInvestmentOrderRequest request) {
        validateQuantity(request.quantity());
        mockInvestmentMarketService.validateOrderableNow();

        Account account = mockInvestmentAccountService.getRequiredAccount(userId);
        Stock stock = findStockByCode(request.stockCode());
        BigDecimal executionPrice = getRequiredCurrentPrice(stock.getStockCode());
        BigDecimal totalExecutionAmount = calculateTotalExecutionAmount(executionPrice, request.quantity());

        validateCashBalance(account, totalExecutionAmount);

        Portfolio portfolio = portfolioRepository.findByAccountIdAndStockId(account.getId(), stock.getId())
                .map(existingPortfolio -> {
                    existingPortfolio.buy(request.quantity(), executionPrice);
                    return existingPortfolio;
                })
                .orElseGet(() -> portfolioRepository.save(
                        Portfolio.create(account, stock, request.quantity(), executionPrice)
                ));

        account.decreaseCashBalance(totalExecutionAmount);
        account.increaseTotalPurchaseAmount(totalExecutionAmount);

        Order order = orderRepository.save(Order.createTrade(
                account,
                stock,
                OrderType.BUY,
                executionPrice,
                request.quantity()
        ));

        return toResponse(order, portfolio, account);
    }

    public MockInvestmentOrderResponse sell(Long userId, MockInvestmentOrderRequest request) {
        validateQuantity(request.quantity());
        mockInvestmentMarketService.validateOrderableNow();

        Account account = mockInvestmentAccountService.getRequiredAccount(userId);
        Stock stock = findStockByCode(request.stockCode());
        Portfolio portfolio = portfolioRepository.findByAccountIdAndStockId(account.getId(), stock.getId())
                .orElseThrow(() -> new CustomException(MockInvestmentErrorCode.INSUFFICIENT_STOCK_QUANTITY));
        validateSellQuantity(portfolio, request.quantity());

        BigDecimal executionPrice = getRequiredCurrentPrice(stock.getStockCode());
        BigDecimal totalExecutionAmount = calculateTotalExecutionAmount(executionPrice, request.quantity());
        BigDecimal purchaseAmountToReduce = portfolio.sell(request.quantity());

        if (portfolio.getQuantity() == 0) {
            portfolioRepository.delete(portfolio);
        }

        account.increaseCashBalance(totalExecutionAmount);
        account.decreaseTotalPurchaseAmount(purchaseAmountToReduce);

        Order order = orderRepository.save(Order.createTrade(
                account,
                stock,
                OrderType.SELL,
                executionPrice,
                request.quantity()
        ));

        return toResponse(order, portfolio, account);
    }

    // ========== 조회 메서드 ==========
    private Stock findStockById(Long stockId) {
        return stockRepository.findById(stockId)
                .orElseThrow(() -> new CustomException(StockErrorCode.STOCK_NOT_FOUND));
    }

    private Stock findStockByCode(String stockCode) {
        return stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new CustomException(StockErrorCode.STOCK_NOT_FOUND));
    }

    private BigDecimal getRequiredCurrentPrice(String stockCode) {
        StockCurrentPriceSnapshot currentPrice = stockCurrentPriceService.getCurrentPrice(stockCode)
                .orElseThrow(() -> new CustomException(MockInvestmentErrorCode.CURRENT_PRICE_NOT_AVAILABLE));
        if (currentPrice.currentPrice() == null) {
            throw new CustomException(MockInvestmentErrorCode.CURRENT_PRICE_NOT_AVAILABLE);
        }
        return currentPrice.currentPrice();
    }

    // ========== 검증 메서드 ==========
    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new CustomException(MockInvestmentErrorCode.INVALID_ORDER_QUANTITY);
        }
    }

    private void validateCashBalance(Account account, BigDecimal totalExecutionAmount) {
        if (account.getCashBalance().compareTo(totalExecutionAmount) < 0) {
            throw new CustomException(MockInvestmentErrorCode.INSUFFICIENT_CASH_BALANCE);
        }
    }

    private void validateSellQuantity(Portfolio portfolio, Integer quantity) {
        if (portfolio.getQuantity() < quantity) {
            throw new CustomException(MockInvestmentErrorCode.INSUFFICIENT_STOCK_QUANTITY);
        }
    }

    // ========== 비즈니스 메서드 ==========
    private BigDecimal calculateTotalExecutionAmount(BigDecimal executionPrice, Integer quantity) {
        return executionPrice.multiply(BigDecimal.valueOf(quantity.longValue()));
    }

    private MockInvestmentOrderResponse toResponse(Order order, Portfolio portfolio, Account account) {
        return new MockInvestmentOrderResponse(
                order.getId(),
                order.getOrderType(),
                order.getStock().getId(),
                order.getStock().getStockCode(),
                order.getStock().getName(),
                order.getQuantity(),
                order.getExecutionPrice(),
                order.getTotalExecutionAmount(),
                account.getCashBalance(),
                account.getTotalPurchaseAmount()
        );
    }
}
