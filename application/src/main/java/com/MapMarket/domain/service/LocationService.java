package com.MapMarket.domain.service;

import com.MapMarket.application.rest.LocationRestAdapter;
import com.MapMarket.application.rest.responseDto.LocationResponseDto;
import com.MapMarket.domain.exception.ProductAlreadyAssignedException;
import com.MapMarket.domain.exception.ResourceNotFoundException;
import com.MapMarket.domain.exception.constants.Constant;
import com.MapMarket.domain.models.Location;
import com.MapMarket.domain.models.Product;
import com.MapMarket.domain.ports.input.FindAllUseCase;
import com.MapMarket.domain.ports.input.LocationUseCase;
import com.MapMarket.domain.ports.output.FindAllOutput;
import com.MapMarket.domain.ports.output.LocationOutputPort;
import com.MapMarket.domain.ports.output.OutputPort;
import com.MapMarket.infrastructure.adapters.output.persistence.mapper.EntityMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

public class LocationService implements LocationUseCase<LocationResponseDto>, FindAllUseCase<LocationResponseDto> {

  private final LocationOutputPort<Location> outputPort;
  private final OutputPort<Product> productOutputPort;
  private final FindAllOutput<Location> findAllOutput;
  private final PagedResourcesAssembler<LocationResponseDto> assembler;
  private final EntityMapper entityMapper;

  public LocationService(
      LocationOutputPort<Location> outputPort,
      OutputPort<Product> productOutputPort,
      FindAllOutput<Location> findAllOutput,
      PagedResourcesAssembler<LocationResponseDto> assembler,
      EntityMapper entityMapper
  ) {
    this.outputPort = outputPort;
    this.productOutputPort = productOutputPort;
    this.findAllOutput = findAllOutput;
    this.assembler = assembler;
    this.entityMapper = entityMapper;
  }

  @Override
  public PagedModel<EntityModel<LocationResponseDto>> findAll(Pageable pageable) {
    Page<Location> allLocations = findAllOutput.findAll(pageable);
    if (allLocations.isEmpty()) throw new ResourceNotFoundException(Constant.LOCATIONS_NOT_FOUND);

    Page<LocationResponseDto> allLocationResponseDto = allLocations.map(
        p -> entityMapper.parseObject(p, LocationResponseDto.class));

    allLocationResponseDto.map(
        p -> p.add(
            linkTo(methodOn(LocationRestAdapter.class)
                .findById(p.getKey())).withSelfRel()));

    Link link = linkTo(
        methodOn(LocationRestAdapter.class)
            .findAll(
                pageable.getPageNumber(),
                pageable.getPageSize()
            )
    ).withSelfRel();
    return assembler.toModel(allLocationResponseDto, link);
  }

  @Override
  public LocationResponseDto findById(Long id) {
    Location location = outputPort.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(Constant.LOCATION_NOT_FOUND + id));

    LocationResponseDto locationDto = entityMapper.parseObject(location, LocationResponseDto.class);
    locationDto.add(linkTo(methodOn(LocationRestAdapter.class).findById(id)).withSelfRel());
    return locationDto;
  }

  @Override
  public LocationResponseDto findLocationByProductId(Long id) {
    Location location = outputPort.findLocationByProductId(id)
        .orElseThrow(() -> new ResourceNotFoundException(Constant.ERROR_PRODUCT_IN_LOCATION_NOT_FOUND + id));

    LocationResponseDto locationDto = entityMapper.parseObject(location, LocationResponseDto.class);
    locationDto.add(linkTo(methodOn(LocationRestAdapter.class).findLocationByProductId(id)).withSelfRel());
    return locationDto;
  }

  @Override
  public LocationResponseDto subscribingProduct(Long locationId, Long productId) {
    existLocationWithProduct(productId);
    existProductInLocation(locationId);
    Location location =  outputPort.findById(locationId)
        .orElseThrow(() -> new ResourceNotFoundException(Constant.LOCATION_NOT_FOUND + locationId));

    Product product =  productOutputPort.findById(productId)
        .orElseThrow(() -> new ResourceNotFoundException(Constant.PRODUCT_NOT_FOUND + productId));

    location.setProduct(product);

    LocationResponseDto locationDto = entityMapper.parseObject(outputPort.subscribingProduct(location), LocationResponseDto.class);
    locationDto.add(linkTo(methodOn(LocationRestAdapter.class).subscribingProduct(locationId, productId)).withSelfRel());
    return locationDto;
  }

  @Override
  public LocationResponseDto unsubscribingProduct(Long locationId) {
    existResource(locationId);
    LocationResponseDto locationDto = entityMapper.parseObject(outputPort.unsubscribingProduct(locationId), LocationResponseDto.class);
    locationDto.add(linkTo(methodOn(LocationRestAdapter.class).unsubscribingProduct(locationId)).withSelfRel());
    return locationDto;
  }

  private void existResource(Long id) {
    if (!outputPort.existResource(id))
      throw new ResourceNotFoundException(Constant.LOCATION_NOT_FOUND + id);
  }

  private void existLocationWithProduct(Long id) {
    if (outputPort.existLocationWithProduct(id))
      throw new ProductAlreadyAssignedException(Constant.THIS_PRODUCT_IS_ALREADY_REGISTERED);
  }

  private void existProductInLocation(Long id) {
    if (outputPort.existProductInLocation(id))
      throw new ProductAlreadyAssignedException(Constant.THIS_LOCATION_WITH_PRODUCT_REGISTERED + id);
  }
}
