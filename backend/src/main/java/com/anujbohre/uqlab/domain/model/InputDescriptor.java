package com.anujbohre.uqlab.domain.model;

/** Describes one input of a simulation model: its name, unit and meaning. */
public record InputDescriptor(String name, String unit, String description) {}
