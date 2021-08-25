package org.emp.data;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Stores vehicle location in OxTS format
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class VehicleLocation {
  float[] oxtsData;
}
