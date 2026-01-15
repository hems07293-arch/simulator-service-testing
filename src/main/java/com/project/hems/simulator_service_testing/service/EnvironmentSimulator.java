package com.project.hems.simulator_service_testing.service;

import org.springframework.stereotype.Component;

@Component
public class EnvironmentSimulator {

    public double calculateSolarProduction() {
        int hour = java.time.LocalTime.now().getHour();

        // Simple window: 6 AM to 6 PM (18:00)
        if (hour < 6 || hour >= 18) {
            return 0.0; // It's night
        }

        // Bell curve: Peaks at noon (12)
        // Formula: MaxPower * sin(pi * (t - 6) / 12)
        double maxPeakW = 5000.0; // 5kW system
        double radians = Math.PI * (hour - 6) / 12.0;

        return maxPeakW * Math.sin(radians);
    }

    public double calculateHomeConsumption() {
        // 1. Base Load: Things that are always on (300W - 500W)
        double baseLoad = 400.0;

        // 2. Random Noise: Small fluctuations (+/- 50W)
        double noise = (Math.random() * 100) - 50;

        // 3. High-Power Spikes: 10% chance a heavy appliance is running (2000W - 4000W)
        double spike = 0.0;
        if (Math.random() < 0.10) {
            spike = 2000.0 + (Math.random() * 2000.0);
        }

        double totalLoad = baseLoad + noise + spike;

        // Ensure we never return a negative consumption
        return Math.max(totalLoad, 100.0);
    }

}
