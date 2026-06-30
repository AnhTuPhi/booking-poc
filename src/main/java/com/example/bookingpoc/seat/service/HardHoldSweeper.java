package com.example.bookingpoc.seat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HardHoldSweeper {

    private static final Logger log = LoggerFactory.getLogger(HardHoldSweeper.class);
    private final SeatService seatService;

    public HardHoldSweeper(SeatService seatService) {
        this.seatService = seatService;
    }

    @Scheduled(fixedDelayString = "${booking.seat.sweep-interval-seconds:30}000")
    public void sweep() {
        int released = seatService.sweepExpired();
        if (released > 0) {
            log.info("Released {} expired hard holds", released);
        }
    }
}
