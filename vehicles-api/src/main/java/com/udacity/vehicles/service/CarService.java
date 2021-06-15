package com.udacity.vehicles.service;

import com.udacity.vehicles.client.maps.MapsClient;
import com.udacity.vehicles.client.prices.Price;
import com.udacity.vehicles.client.prices.PriceClient;
import com.udacity.vehicles.domain.Location;
import com.udacity.vehicles.domain.car.Car;
import com.udacity.vehicles.domain.car.CarRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

/**
 * Implements the car service create, read, update or delete
 * information about vehicles, as well as gather related
 * location and price data when desired.
 */
@Service
public class CarService {

    private final CarRepository repository;
    private final MapsClient mapsClient;
    private final PriceClient priceClient;

    public CarService(CarRepository repository, MapsClient mapsClient, PriceClient priceClient) {
        this.repository = repository;
        this.mapsClient = mapsClient;
        this.priceClient = priceClient;
    }

    /**
     * Gathers a list of all vehicles
     * @return a list of all vehicles in the CarRepository
     */
    public List<Car> list() {
        return repository.findAll()
                .stream()
                .peek(car -> {
                    car.setPrice(priceClient.getPrice(car.getId()));
                    car.setLocation(mapsClient.getAddress(car.getLocation()));
                }).collect(Collectors.toList());
    }

    /**
     * Gets car information by ID (or throws exception if non-existent)
     * @param id the ID number of the car to gather information on
     * @return the requested car's information, including location and price
     */
    public Car findById(Long id) {
        Optional<Car> carOptional = repository.findById(id);
        if (carOptional.isEmpty()) {
            throw new CarNotFoundException();
        }

        /**
         * Note: The car class file uses @transient, meaning you will need to call
         *   the pricing service each time to get the price.
         */

        Car car = carOptional.get();
        car.setPrice(priceClient.getPrice(id));

        /**
         * Note: The Location class file also uses @transient for the address,
         * meaning the Maps service needs to be called each time for the address.
         */

        Location location = mapsClient.getAddress(car.getLocation());
        car.setLocation(location);
        return car;
    }

    /**
     * Either creates or updates a vehicle, based on prior existence of car
     * @param car A car object, which can be either new or existing
     * @return the new/updated car is stored in the repository
     */
    public Car save(Car car) {
        if (car.getId() != null) {
            savePrice(car.getPrice(), car.getId());

            return repository.findById(car.getId())
                    .map(carToBeUpdated -> {
                        carToBeUpdated.setDetails(car.getDetails());
                        carToBeUpdated.setLocation(car.getLocation());
                        carToBeUpdated.setCondition(car.getCondition());
                        return repository.save(carToBeUpdated);
                    }).orElseThrow(CarNotFoundException::new);
        }

        Car savedCar = repository.save(car);
        savePrice(car.getPrice(), savedCar.getId());
        return savedCar;
    }

    public void savePrice(String price, Long vehicleId) {
        try {
            BigDecimal priceBigDecimal = BigDecimal.valueOf(Long.parseLong(price));
            priceClient.savePrice(new Price("USD", priceBigDecimal, vehicleId));
        } catch (NumberFormatException e) {
            e.printStackTrace();

            throw new InvalidPriceException();
        }
    }

    /**
     * Deletes a given car by ID
     * @param id the ID number of the car to delete
     */
    public void delete(Long id) {
        Optional<Car> carOptional = repository.findById(id);
        if (carOptional.isEmpty()) {
            throw new CarNotFoundException();
        }

        priceClient.deletePrice(carOptional.get().getId());
        repository.delete(carOptional.get());
    }
}
