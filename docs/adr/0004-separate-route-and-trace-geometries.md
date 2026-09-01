# ADR-0004: Keep route geometry and trace edge-index geometry separate

Status: Accepted  
Type: Retrospective reconstruction

## Context

Valhalla supplies two geometrically related but semantically different representations during connector construction:

1. `/route` returns the routed transfer shape.
2. `/trace_attributes` with `shape_match=edge_walk` returns edge attributes plus the matched shape whose `begin_shape_index` / `end_shape_index` values index that returned trace shape.

The preserved `trail-plan-1.scala` state already contains the core safety lesson behind this separation:

- `RouteResult.points` is the elevated/resampled route profile used for route-level rider/terrain calculations;
- `TraceAttributesDetailed.shape` is the matched edge-walk shape used for shape-indexed edge safety;
- road/downhill classification validates every edge range against the trace shape;
- the source explicitly warns not to project the trace shape back onto `route.points`, because nearest-point projection is ambiguous at self-intersections and overpasses and may transfer elevation from the wrong traversal.

The preserved `trail-plan-2.scala` state makes this boundary explicit in the connector model itself:

- `Connector.geometry` is the dense `/route` geometry with Valhalla elevation;
- `Connector.traceGeometry` is the `/trace_attributes` edge-walk geometry with its own elevation;
- `EdgeAttr.begin` / `EdgeAttr.end` refer only to `traceGeometry`;
- road stress is computed from `edges` against `traceGeometry`;
- wall, rider physics, ascent, real-ride evidence, protected-corridor final checks, and GPX reconstruction use `geometry`.

The two geometries may describe the same physical transfer, but they do not share one index space and are not interchangeable representations.

## Decision

Keep route geometry and trace edge-index geometry as separate canonical connector representations.

Specifically:

1. The connector's route geometry is owned by the `/route` path, after the planner's canonical route resampling/elevation processing.
2. The trace geometry is owned by the `/trace_attributes` `edge_walk` response and its own `/height` elevation.
3. `EdgeAttr.begin` and `EdgeAttr.end` are valid only in the trace geometry's index space.
4. Road classification, contiguous edge-run extraction, modeled road-run duration, and road stress must use `EdgeAttr` together with `traceGeometry`.
5. Wall metrics, rider physics, ascent, real-ride evidence application, protected-corridor final checks, and GPX reconstruction must use the route `geometry`.
6. No code may apply trace edge indices to route geometry.
7. No cleanup may replace route geometry with trace geometry merely to reduce the number of representations.
8. No nearest-point or similar projection may be used to manufacture a shared index space between the two geometries.

## Consequences

### Positive

- Edge indices are always interpreted in the coordinate sequence that produced them.
- Road-safety calculations remain stable at self-intersections, overpasses, close parallel roads, and other geometrically ambiguous locations.
- Route-level wall/rider/reconstruction semantics remain tied to the routed transfer geometry rather than to a trace-matching representation.
- `/height` elevation is requested independently for the geometry whose semantics require it, avoiding accidental elevation transfer between traversals.
- Representation ownership is explicit enough to audit: every metric can be traced to the correct geometry.

### Constraints

- `Connector` must retain both representations when both route-level and edge-indexed semantics are required.
- Functions consuming `EdgeAttr.begin/end` must also consume the matching `traceGeometry`, not a generic `Vector[Point]` whose provenance is unclear.
- Reconstruction must not use `traceGeometry` as a substitute for routed connector geometry.
- Wall and rider-physics calculations must not use edge-walk trace geometry merely because elevation is available there.
- Any future attempt to collapse the geometries requires proof that the two services return one identical point sequence and index space for all supported inputs; similarity or near-coincidence is insufficient.

## Rejected alternatives

### Apply `begin_shape_index` / `end_shape_index` to route geometry

Rejected because those indices are defined by the `/trace_attributes` returned shape, not by the independently processed `/route` geometry.

### Re-project trace edges onto route geometry

Rejected. The preserved state-1 implementation explicitly records that nearest-point projection is ambiguous at self-intersections and overpasses and can associate elevation or traversal geometry with the wrong passage.

### Use trace geometry for all connector semantics

Rejected. The trace shape exists to support edge-walk/indexed road semantics; it is not the canonical routed representation for wall, rider physics, protected-corridor final checks, or reconstruction.

### Use route geometry for all connector semantics

Rejected because route resampling/elevation changes its point sequence and therefore cannot safely host indices emitted for the trace shape.

### Treat the two geometries as interchangeable because they describe the same transfer

Rejected. Geographic similarity does not imply identity of shape sequence, vertex count, elevation sampling, or index ownership.

## Historical evidence

This ADR was reconstructed from preserved source states.

### Previous state: `trail-plan-1.scala`

Relevant preserved behavior includes:

- `/route` returns `routePoints`, which are separately resampled/elevated into `RouteResult.points`;
- `RouteResult` also keeps the raw Valhalla route shape;
- `/trace_attributes` returns its own matched shape and edge `begin_shape_index` / `end_shape_index`;
- `traceAttributesDetailed` elevates the matched trace shape independently;
- road-safety code validates and slices edge runs against that trace shape;
- the source explicitly states that the exact edge-walk matched shape must not be re-projected onto `route.points` because projection is ambiguous at self-intersections/overpasses.

This means the core separation already existed before the greenfield rewrite.

### Next state: `trail-plan-2.scala`

Build ID in the preserved snapshot:

`PRODUCT-V6-GREENFIELD1-FIX44-BLOCKER-DEAD-EVIDENCE-CLOSED`

The connector model makes the ownership boundary explicit:

- `geometry`: dense `/route` geometry with Valhalla elevation; canonical for wall/physics/ascent/evidence and GPX reconstruction;
- `traceGeometry`: exact edge-walk shape with its own elevation;
- `EdgeAttr.begin/end` refer only to `traceGeometry`;
- road stress is calculated from `edges` and `traceGeometry`;
- final protected-corridor overlap, ascent, rider metrics, wall metrics, and reconstruction continue to use route `geometry`.

## Retrospective note

This ADR was written retrospectively from preserved source states.

The architectural decision did not first appear as one atomic code change between the two snapshots: `trail-plan-1.scala` already contains the essential index-space separation and the explicit warning against re-projection. The `trail-plan-2.scala` rewrite promotes that lesson into an explicit connector representation boundary.

This ADR therefore records the durable architecture represented across that transition; it does **not** claim that the decision originated only in `trail-plan-2.scala`, nor that this ADR file existed when the original code was written.
