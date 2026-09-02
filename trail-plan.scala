#!/usr/bin/env -S instant-scala
//> using scala "3.3.7"
//> using dep "com.lihaoyi::ujson:4.4.3"
//> using jvm "21"
//> using packaging.graalvmArgs --no-fallback

// ============================================================================
// TEST-ONLY NOTES + CLOSED EVIDENCE HISTORY
// ============================================================================
// Historical notes below are non-authoritative context. Production semantics are
// defined by executable behavior/tests and the repository's canonical owners.
//
// [TEST-ONLY CONTRACT SUITE REVIEW] FIX46 keeps exactly 22 default-running
//        tests but rebalances them around product contracts instead of historical
//        micro-fixes. Added direct coverage for CLI default/--no-test semantics,
//        canonical input counts/identity, all three hard-wall thresholds,
//        hard-vs-scored road policy, exact RAW complete-order behavior,
//        mandatory exactly-once supplied-direction reconstruction, rider-selector
//        guardrails, the human-report/exact-five-output-files contract, and the
//        final reconstructed GPX 100m WARN / 250m FAIL thresholds. Historical
//        regressions that still encode real contracts (corridor clipping,
//        canonical <=10m safety sampling, real-ride direction, technical downhill
//        policy, no-horizon exact rider search) remain. Tests are NOT scheduled
//        for deletion; eventual extraction to a separate test source remains the
//        cleanup target.
//
// [CLOSED / VALIDATED] FIX53 production12/local-selector adjudication:
//        Canonical run passed 22/22 and E2E PASS. Restoring production12 reduced
//        graph 1607->706-era evidence work back to accepted=1206/retained=540,
//        and total runtime from ~108s (FIX52 candidate16) to ~69.4s while keeping
//        exactly the same local-selector product as FIX51/FIX52:
//          C1 7077.9s / 699.1s  (~1:57:58 / 11:39)
//          C2 6921.4s / 906.9s  (~1:55:21 / 15:07)
//          C3 7552.6s / 1102.3s (~2:05:53 / 18:22)
//        Wall classes and LOOP/LOOP/P2P assignment were unchanged. This closes
//        the candidate16 production question for the current product/inputs.
//        Reopen midpoint-cover work only on new product requirements/evidence.
//
// [CLOSED / REMOVED] FIX45-FIX52 profile/selector evidence:
//        - FIX45 found 14 useful midpoint-derived semantic connector groups
//          outside production12; exact missing-group cover needed 4 profiles.
//        - FIX48/FIX49 let those 4 profiles into the real graph: 188 retained
//          added variants survived pruning.
//        - FIX50 proved the old global-extrema-normalized knee was search-space
//          unstable: far comfort-tail additions moved C2 despite the selected
//          route using no added connector.
//        - FIX51 promoted local-marginal-drop; FIX52 promoted the reproduced
//          search-space-extension failure into permanent default test #19.
//        - FIX52 regression run passed 22/22 and selected C1/C2/C3 used ZERO of
//          the 4 added profiles in both RAW baselines and final routes.
//        Therefore FIX53 restores production12, removes candidate16 attribution
//        and FIX50 frontier/prefix/turning-angle runtime diagnostics, and keeps
//        local-marginal-drop plus the regression test. Revisit midpoint cover
//        only if product requirements change or new evidence shows a selected
//        route benefits from those additional semantic groups.
//
// [TEST-ONLY] In-file self-test harness and synthetic fixtures (`runSelfTests`,
//        selftest-no-horizon, geometry/road/DP fixtures).
//        Current product contract requires tests to run by default, so DO NOT
//        delete them yet. If/when they move to a separate test artifact, remove
//        the embedded harness and fixtures from the production source in the same
//        change; retain only `--no-test` behaviour if still required.
//
// Already removed temporary evidence:
// - FIX31 `road-primary-evidence` per-connector logging.
// - FIX34 historical OLD corridor matcher and all OLD-vs-corrected matcher/
//   blocker differential counters/logging.
// - FIX34 targeted `corridor-safety-sampling` log for
//   Bunker -> RegenbogenAbzweiger.
// - FIX34 `trace-corridor-diagnostic` comparison logging.
// - FIX34 successful-route `reroute-chain` logging. Failure/cap diagnostics remain.
// - FIX34 RAW 3x2 class/endpoint `evidence-matrix` type/computation/output.
// - FIX35 `GRAPH TRANSITION x CLASS INVENTORY` per-transition/class dump.
// - FIX35 full RAW breakpoint signatures; retained compact breakpoint metrics.
// - FIX35 per-layer and terminal-progress rider-DP telemetry; retained final
//   rider-DP timing/counter summaries.
// - FIX36 verbose `rider-policy-evidence` orders/fractions. A compact
//   FAST/KNEE/COMFORT summary was retained then, and was later removed in FIX53
//   after FIX50 selector evidence closed; production keeps only
//   `rider-policy-selected`.
// - FIX36 RAW frontier bottleneck/connector-physics detail; retained compact
//   wall/transfer/road/order diagnostics for exact RAW breakpoints.
// - FIX38 temporary 25-profile cover audit, per-profile cost/yield accounting,
//   exact set-cover diagnostics and audit-only profile generation. The audit
//   proved under current connector/safety/road semantics that the production
//   12 profiles cover all 249 useful solver-semantic groups found by the
//   historical calibrated 25-profile superset; exact set-cover size is still
//   12, with 0 jointly removable production profiles and 0 missing useful
//   groups requiring audit-profile additions.
// - FIX40 temporary global-25 exact-cover / zero-transition audit. It proved
//   the global exact minimum remains 12 profiles. It also found the same 19
//   logical transitions finish with no accepted connector for all 25 calibrated
//   profiles, but FIX41 subsequently proved these are NOT base Valhalla
//   reachability holes.
// - FIX41 temporary one-profile base-route topology scout. Result: all 120
//   logical transitions have a base Valhalla route on the validated fingerprint
//   (scoutNoRoute=0); the 19 empty graph transitions only become no-route after
//   protected-corridor blocker/safety rerouting. Therefore no production
//   profile attempts can be skipped from a cheap base-route scout. The scout
//   saved 0 attempts and slightly increased graph time (~36.31s -> ~36.49s);
//   all scout code/counters/fingerprint gating were removed.
// - FIX43 temporary blocker-dead evidence. Result on production12:
//   19 empty transitions x 12 profiles = 228 blocker-dead connector attempts,
//   consuming 1546 /route requests and ~8.674s measured connector time.
//   Most die after 6 blocker reroutes (a few after 4-5), but blocker sequences
//   are not profile-invariant: several transitions have 4-7 distinct sequences
//   and Reiherberg->LittleWhistler / LittleWhistlerB each have 10.
//   Therefore cross-profile "first dead profile => skip the rest" is NOT a
//   justified production rule. Likewise batching additional avoid_locations
//   would change point-blocker search semantics and can suppress legitimate
//   alternatives, so no such shortcut was promoted. All FIX43 evidence code was
//   removed after this result was recorded.
// ============================================================================

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import java.text.Normalizer
import java.time.{Duration, Instant}
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.{Try, Success, Failure}
import ujson.*

object MtbRoutePlanner:
  val BuildId = "PRODUCT-V6-GREENFIELD1-FIX54-FREEZE-CANDIDATE"
  val OutputGpxFiles:Vector[String] = Vector("day.gpx","day.wall-c2.gpx","day.wall-c3.gpx")
  val OutputSummaryFile:String = "day.txt"
  val OutputDebugFile:String = "day.debug.txt"
  val OutputFileSet:Vector[String] = OutputGpxFiles ++ Vector(OutputSummaryFile,OutputDebugFile)

  val FinalGapWarnM:Double = 100.0
  val FinalGapFailM:Double = 250.0
  def finalGapLevel(distanceM:Double):Int =
    if distanceM >= FinalGapFailM then 2
    else if distanceM >= FinalGapWarnM then 1
    else 0

  val EarthR = 6371008.8
  val AvoidToleranceM = 12.0
  val Start = Point(53.472143, 9.876907, 0.0)
  val LoopFinish = Point(53.472143, 9.876907, 0.0)
  val P2PFinish = Point(53.465204, 9.962392, 0.0)
  val StartName = "S-Neuwiedenthal"
  val P2PFinishName = "S-Heimfeld"

  val RiderMass = 65.0
  val BikeMass = 20.0
  val TotalMass = RiderMass + BikeMass
  val WheelInch = 27.5
  val FrontTeeth = 32.0
  val RearTeeth = 51.0
  val MinimumCadence = 45.0
  val DrivetrainEfficiency = 0.95
  val TargetPower = 80.0
  val G = 9.80665
  val Rho = 1.225
  val CdA = 0.60
  val PhysicsGradeWindowM = 30.0
  val TrailDownhillMaxKph = 6.0
  // Rider-quality search has no fixed time horizon. Exact pruning is limited to
  // resources that are monotone and already make RAW-baseline upgrade eligibility
  // impossible; transfer is never capped by a fixed detour budget.
  //
  // Rider product selection is applied only AFTER exact search. Among all
  // guard-safe strict candHard upgrades we build the exact 2-D Pareto frontier
  // (transfer, candHard). Since FIX51 production selects the LOCAL marginal-drop
  // elbow: compare neighboring frontier segment slopes (candHard seconds gained
  // per additional transfer second) and select the point immediately before the
  // strongest local collapse in marginal benefit. Transfer and candHard share
  // the same unit (modeled seconds), so no global endpoint normalization is
  // required. FIX50/FIX52 established and regression-test that a far low-benefit
  // comfort tail must not move an unchanged local elbow. No +60/+600 window,
  // migration route, fixed time horizon, weighted score, percentage detour
  // budget, beam, top-K or epsilon search approximation is introduced.
  //
  // ROAD POLICY (validated by FIX31/FIX32 A/B): finite unprotected-primary
  // exposure is measured road stress, not a duration-threshold hard deletion.
  // Motorway/trunk/steps/ferry/rail/impassable remain hard; unmodelable primary
  // duration remains fail-closed.

  // HUMAN REPORT ONLY.  Keep ride optimization independent from stop planning.
  // This preserves the useful old day.txt convention explicitly: 3 minutes
  // before and 3 minutes after each mandatory technical GPX.  With the current
  // 10 mandatory trails this adds 60 minutes to modeled moving time.  It does
  // not affect connector generation, DP, wall classes, rider metrics or GPX.
  val HumanReportTrailPauseMin = 3.0

  case class Point(lat: Double, lon: Double, ele: Double)
  case class XY(x: Double, y: Double)
  case class Profile(speedKph: Double, useHills: Double, useRoads: Double):
    def id: String = f"v${speedKph}%.0f-h${useHills}%.2f-r${useRoads}%.2f"

  // Production Valhalla search cover.
  // FIX38 revalidated this set against the historically calibrated 25-profile
  // superset under the CURRENT connector/safety/road semantics:
  //   useful solver-semantic groups: production12=249, historical25=249
  //   missing useful groups outside production12: 0
  //   exact set-cover over current12: size=12
  //   jointly removable production profiles: 0
  // Therefore every profile below remains necessary for preserving the current
  // useful connector-class cover, while the 13 historical audit-only profiles
  // add no new useful solver-semantic group.
  // Production profile cover. FIX45-FIX52 midpoint evidence is recorded in
  // the cleanup ledger above; the four audit-only additions were removed after
  // they proved unused by RAW and final routes under the stable local selector.
  val Profiles = Vector(
    Profile(20.0, 0.25, 0.35), Profile(20.0, 0.50, 0.35), Profile(20.0, 0.90, 0.35),
    Profile(15.0, 0.05, 0.35), Profile(15.0, 0.50, 0.75), Profile(15.0, 0.90, 0.35),
    Profile(15.0, 0.90, 0.75), Profile(25.0, 0.05, 0.35), Profile(25.0, 0.90, 0.35),
    Profile(15.0, 0.70, 0.55), Profile(25.0, 0.16, 0.75), Profile(20.0, 0.98, 0.55)
  )

  case class DemandingMeasurements(
      wholeGradePct: Double,
      wholeSinuosity: Double,
      local60MaxGradePct: Double,
      local60MaxSinuosity: Double,
      local60Pass: Boolean,
      local100MaxGradePct: Double,
      local100MaxSinuosity: Double,
      local100Pass: Boolean
  ):
    def demanding: Boolean =
      (wholeGradePct >= 10.0 && wholeSinuosity >= 1.10) || local60Pass || local100Pass

  case class Trail(name: String, points: Vector[Point], demanding: DemandingMeasurements, rider: RiderMetrics)
  case class Gpx(name: String, points: Vector[Point])
  case class GeoBounds(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double)

  case class ProtectedCorridor(name: String, points: Vector[Point], kind: String):
    def label: String = s"$kind:$name"
    lazy val bounds: GeoBounds = geoBounds(points)

  case class EdgeAttr(
      id: String,
      begin: Int,
      end: Int,
      lengthM: Double,
      speedKph: Double,
      roadClass: String,
      use: String,
      surface: String,
      cycleLane: String,
      unpaved: Boolean,
      ridingSeconds: Double
  ):
    def seconds: Double =
      if speedKph > 0 then lengthM / (speedKph / 3.6)
      else if ridingSeconds.isFinite && ridingSeconds >= 0 then ridingSeconds
      else Double.PositiveInfinity

  case class Streak(prefix: Double, suffix: Double, localMax: Double, allAbove: Boolean, duration: Double):
    def concat(b: Streak): Streak =
      val d = duration + b.duration
      val all = allAbove && b.allAbove
      val pre = if allAbove then duration + b.prefix else prefix
      val suf = if b.allAbove then b.duration + suffix else b.suffix
      val cross = suffix + b.prefix
      Streak(pre, suf, math.max(math.max(localMax, b.localMax), cross), all, d)

  object Streak:
    val Empty = Streak(0.0, 0.0, 0.0, true, 0.0)
    def constant(seconds: Double, above: Boolean): Streak =
      if seconds <= 0 then Empty
      else if above then Streak(seconds, seconds, seconds, true, seconds)
      else Streak(0.0, 0.0, 0.0, false, seconds)

  case class RiderMetrics(
      duration: Double,
      t120: Double,
      t140: Double,
      t160: Double,
      streak120: Streak,
      streak140: Streak,
      streak180: Streak,
      spike: Double
  ):
    def candHard: Double = t120 + t140 + t160
    def concat(b: RiderMetrics): RiderMetrics = RiderMetrics(
      duration + b.duration,
      t120 + b.t120,
      t140 + b.t140,
      t160 + b.t160,
      streak120.concat(b.streak120),
      streak140.concat(b.streak140),
      streak180.concat(b.streak180),
      spike + b.spike
    )

  object RiderMetrics:
    val Empty = RiderMetrics(0,0,0,0,Streak.Empty,Streak.Empty,Streak.Empty,0)

  case class WallMetrics(max30Pct: Double, max100Pct: Double, above180Seconds: Double):
    def physicalSeverity: Double =
      def safe(v: Double): Double = if v.isFinite && v > 0 then v else 0.0
      math.max(math.max(safe(max30Pct) / 27.0, safe(max100Pct) / 20.0), safe(above180Seconds) / 90.0)
    def hardInvalid: Boolean = max30Pct >= 27.0 || max100Pct >= 20.0 || above180Seconds >= 90.0

  case class EvidenceCandidate(corridor: String, windowM: Double, s: Double, grade1Pct: Double, grade2Pct: Double, commonPct: Double)
  case class EvidenceApplication(corridor: String, severity: Double, details: Vector[String])

  case class Connector(
      id: String,
      from: String,
      to: String,
      profile: Profile,
      // Dense (10 m) /route geometry with Valhalla elevation. This is the
      // canonical connector profile for wall/physics/ascent/evidence and GPX
      // reconstruction. Keep it independent from trace shape indices.
      geometry: Vector[Point],
      // Exact edge_walk shape with its own elevation. EdgeAttr begin/end indices
      // refer ONLY to this geometry and road-safety code must use it.
      traceGeometry: Vector[Point],
      rawSeconds: Double,
      edges: Vector[EdgeAttr],
      roadStressSeconds: Double,
      ascentM: Double,
      crr: Double,
      rider: RiderMetrics,
      wall: WallMetrics,
      physicalWall: Double,
      evidenceFloor: Double,
      effectiveWall: Double,
      evidence: Vector[EvidenceApplication],
      avoidWarnings: Vector[(String, Double)],
      safetyProvenance: Vector[String]
  )

  enum Mode:
    case LOOP, P2P
    def finishKey: String = this match
      case LOOP => "FINISH_LOOP"
      case P2P => "FINISH_P2P"
    def finishPoint: Point = this match
      case LOOP => LoopFinish
      case P2P => P2PFinish
  case class RawLabel(mask: Int, last: Int, wall: Double, transfer: Double, road: Double, order: Vector[Int], connectors: Vector[Connector]):
    def signature(trails: Vector[Trail], mode: Option[Mode] = None): String =
      val base = order.map(i => trails(i).name).mkString("->") + "|" + connectors.map(_.id).mkString(";")
      mode.fold(base)(m => base + "|" + m.toString)

  case class RawTerminal(mode: Mode, wall: Double, transfer: Double, road: Double, order: Vector[Int], connectors: Vector[Connector], signature: String)
  case class Breakpoint(mode: Mode, ceiling: Double, transfer: Double, road: Double, signature: String)

  case class ClimbState(count: Int, maxAscent: Double, upward: Double, roughness: Double, prevAscent: Option[Double], prevDelta: Option[Double]):
    def add(ascent: Double, connectorOrdinal: Int): ClimbState =
      if connectorOrdinal < 2 then this
      else prevAscent match
        case None => ClimbState(1, ascent, upward, roughness, Some(ascent), None)
        case Some(p) =>
          val d = ascent - p
          val up2 = upward + math.max(0.0, d)
          val rough2 = roughness + prevDelta.map(pd => math.abs(d - pd)).getOrElse(0.0)
          ClimbState(count + 1, math.max(maxAscent, ascent), up2, rough2, Some(ascent), Some(d))

  object ClimbState:
    val Empty = ClimbState(0,0,0,0,None,None)

  case class RiderLabel(
      mask: Int,
      last: Int,
      transfer: Double,
      road: Double,
      requiredWall: Double,
      rider: RiderMetrics,
      climb: ClimbState,
      warmupPenalty: Int,
      demandingAdjacency: Int,
      order: Vector[Int],
      connectors: Vector[Connector]
  ):
    def signature(trails: Vector[Trail]): String = order.map(i => trails(i).name).mkString("->") + "|" + connectors.map(_.id).mkString(";")

  case class RiderTerminal(
      mode: Mode,
      transfer: Double,
      road: Double,
      requiredWall: Double,
      rider: RiderMetrics,
      climb: ClimbState,
      warmupPenalty: Int,
      demandingAdjacency: Int,
      order: Vector[Int],
      connectors: Vector[Connector],
      signature: String
  )

  case class AuditResult(failures: Vector[String], warnings: Vector[String]):
    def status: String = if failures.nonEmpty then "FAIL" else if warnings.nonEmpty then "WARN" else "PASS"

  case class Diagnostics(
      var generated: Long = 0,
      var noRoute: Long = 0,
      var retained: Long = 0,
      var acceptedVariants: Long = 0,
      hardRejects: mutable.Map[String, Long] = mutable.Map.empty.withDefaultValue(0L),
      var evidenceApplied: Long = 0,
      var safetyReroutes: Long = 0,
      var safetyBlockedProfiles: Long = 0,
      safetyRerouteCorridors: mutable.Map[String, Long] = mutable.Map.empty.withDefaultValue(0L),
      rawFrontierSizes: mutable.Map[String, Int] = mutable.Map.empty,
      riderFrontierSizes: mutable.Map[String, Int] = mutable.Map.empty,
      timings: mutable.ArrayBuffer[(String, Double)] = mutable.ArrayBuffer.empty
  ):
    def reject(reason: String): Unit = hardRejects(reason) = hardRejects(reason) + 1
  case class Cli(
      input: Option[Path],
      output: Option[Path],
      valhallaUrl: Option[String],
      runTests: Boolean,
      help: Boolean
  )

  type Result[A] = Either[String,A]

  def boundary[A](label:String)(body: => A):Result[A] =
    Try(body).toEither.left.map { e =>
      val detail=Option(e.getMessage).filter(_.nonEmpty).getOrElse(e.getClass.getSimpleName)
      s"$label: $detail"
    }

  def parseCli(args:Array[String]):Result[Cli] =
    def loop(
        i:Int,
        input:Option[Path],
        output:Option[Path],
        url:Option[String],
        runTests:Boolean,
        help:Boolean
    ):Result[Cli] =
      if i >= args.length then Right(Cli(input,output,url,runTests,help))
      else
        args(i) match
          case "--input" =>
            if i + 1 >= args.length then Left("--input requires DIR")
            else loop(i+2,Some(Paths.get(args(i+1))),output,url,runTests,help)
          case "--output" =>
            if i + 1 >= args.length then Left("--output requires DIR")
            else loop(i+2,input,Some(Paths.get(args(i+1))),url,runTests,help)
          case "--valhalla-url" =>
            if i + 1 >= args.length then Left("--valhalla-url requires URL")
            else loop(i+2,input,output,Some(args(i+1).stripSuffix("/")),runTests,help)
          case "--no-test" => loop(i+1,input,output,url,false,help)
          case "--help" | "-h" => loop(i+1,input,output,url,runTests,true)
          case "--self-test" => Left("--self-test was removed; tests run by default, use --no-test to skip them")
          case other => Left(s"unknown argument: $other")

    loop(0,None,None,None,true,false)


  def nfc(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFC)

  def haversine(a: Point, b: Point): Double =
    val la1 = math.toRadians(a.lat); val la2 = math.toRadians(b.lat)
    val dlat = la2 - la1; val dlon = math.toRadians(b.lon - a.lon)
    val h = math.sin(dlat/2)*math.sin(dlat/2) + math.cos(la1)*math.cos(la2)*math.sin(dlon/2)*math.sin(dlon/2)
    2.0 * EarthR * math.asin(math.min(1.0, math.sqrt(h)))

  def cumulative(points: Vector[Point]): Vector[Double] =
    val out = Array.ofDim[Double](points.size)
    var i = 1
    while i < points.size do
      out(i) = out(i-1) + haversine(points(i-1), points(i)); i += 1
    out.toVector

  def interpolate(points: Vector[Point], cum: Vector[Double], s: Double): Point =
    if s <= 0 then points.head
    else if s >= cum.last then points.last
    else
      var lo = 0; var hi = cum.size - 1
      while lo + 1 < hi do
        val mid = (lo + hi) >>> 1
        if cum(mid) <= s then lo = mid else hi = mid
      val ds = cum(lo+1) - cum(lo)
      val t = if ds <= 0 then 0.0 else (s - cum(lo)) / ds
      val a = points(lo); val b = points(lo+1)
      Point(a.lat + t*(b.lat-a.lat), a.lon + t*(b.lon-a.lon), a.ele + t*(b.ele-a.ele))

  def slicePolyline(points: Vector[Point], fromS: Double, toS: Double): Vector[Point] =
    val cum = cumulative(points)
    if points.size < 2 || toS <= fromS || fromS < 0 || toS > cum.last + 1e-9 then Vector.empty
    else
      val out = mutable.ArrayBuffer.empty[Point]
      out += interpolate(points,cum,fromS)
      points.indices.foreach { i => if cum(i) > fromS + 1e-9 && cum(i) < toS - 1e-9 then out += points(i) }
      out += interpolate(points,cum,toS)
      dedupeBoundary(out.toVector)

  def samplePolyline(points:Vector[Point],spacingM:Double):Vector[Point] =
    if points.size<2 || spacingM<=0 then points
    else
      val cum=cumulative(points)
      if cum.last<=1e-9 then Vector(points.head,points.last)
      else
        val distances=mutable.ArrayBuffer.empty[Double]
        var s=0.0
        while s<cum.last do
          distances += s
          s += spacingM
        distances += cum.last
        dedupeBoundary(distances.toVector.map(d => interpolate(points,cum,d)))

  /** Canonical connector-physics resampling: subdivide every raw Valhalla
   * segment independently so every original shape vertex survives /height. */
  def resampleConnectorPhysics(points:Vector[Point],spacingM:Double):Vector[Point] =
    if points.size<2 || spacingM<=0 then points
    else
      val generated=points.sliding(2).flatMap {
        case Vector(a,b) =>
          val segmentM=haversine(a,b)
          if segmentM<=0.0 then Iterator.empty
          else
            val steps=math.max(1,math.ceil(segmentM/spacingM).toInt)
            (1 to steps).iterator.map { k =>
              val t=k.toDouble/steps.toDouble
              Point(
                a.lat+(b.lat-a.lat)*t,
                a.lon+(b.lon-a.lon)*t,
                a.ele+(b.ele-a.ele)*t
              )
            }
        case _ => Iterator.empty
      }.toVector
      val result=points.head +: generated
      if result.size>=2 && haversine(result(result.size-2),result.last)<0.05 then
        result.dropRight(1) :+ points.last
      else result

  /** Canonical geometry for protected-corridor policy.
   *
   * The co-travel classifier is intentionally segment-local (the 0.55
   * along-progress test is evaluated on clipped route segments), therefore
   * feeding arbitrary raw Valhalla segment lengths makes the safety decision
   * representation-dependent.  Production normalizes every /route shape to
   * the same segment-wise <=10 m geometry before overlap measurement AND
   * blocker selection.  This is the same geometric sampling contract used by
   * connector physics/elevation and preserves every raw Valhalla vertex.
   */
  def corridorSafetyGeometry(points: Vector[Point]): Vector[Point] =
    resampleConnectorPhysics(points, 10.0)

  def exactWindowStarts(cum: Vector[Double], window: Double): Vector[Double] =
    if cum.isEmpty || cum.last + 1e-9 < window then Vector.empty
    else
      val maxStart = cum.last - window
      val s = mutable.TreeSet.empty[Double]
      s += 0.0; s += maxStart
      cum.foreach { x =>
        if x >= 0 && x <= maxStart then s += x
        val y = x - window
        if y >= 0 && y <= maxStart then s += y
      }
      s.toVector

  def maxExactUphillGrade(points: Vector[Point], window: Double): Double =
    if points.size < 2 then 0.0
    else
      val cum = cumulative(points)
      exactWindowStarts(cum, window).foldLeft(0.0) { (best,s) =>
        val a = interpolate(points,cum,s); val b = interpolate(points,cum,s+window)
        math.max(best, 100.0 * (b.ele - a.ele) / window)
      }

  def demandingMeasurements(points: Vector[Point]): DemandingMeasurements =
    val cum = cumulative(points)
    val endpoint = haversine(points.head, points.last)
    // Canonical reference behavior uses travelled horizontal polyline length.
    // The greenfield prompt also states an endpoint-chord denominator, but that
    // contradicts its own canonical demanding-set regression: Autobahn would
    // become demanding (~10.9% chord-normalized) instead of the required two-trail
    // set. Preserve the executable canonical contract rather than hardcoding names.
    val wholeGrade =
      if cum.last > 1e-9 then 100.0 * math.max(0.0, points.head.ele - points.last.ele) / cum.last
      else 0.0
    val wholeSinu =
      if cum.last <= 1e-9 then 1.0
      else if endpoint <= 1e-6 then Double.PositiveInfinity
      else cum.last / endpoint
    def local(window: Double, gradeThreshold: Double): (Double,Double,Boolean) =
      var maxGrade = 0.0; var maxSinu = 0.0; var pass = false
      exactWindowStarts(cum,window).foreach { s =>
        val a=interpolate(points,cum,s); val b=interpolate(points,cum,s+window)
        val straight = haversine(a,b)
        val g = 100.0 * math.max(0.0,a.ele-b.ele)/window
        val si = if straight > 0 then window/straight else Double.PositiveInfinity
        maxGrade=math.max(maxGrade,g); maxSinu=math.max(maxSinu,si)
        if g >= gradeThreshold && si >= 1.20 then pass=true
      }
      (maxGrade,maxSinu,pass)
    val l60=local(60,18); val l100=local(100,15)
    DemandingMeasurements(wholeGrade,wholeSinu,l60._1,l60._2,l60._3,l100._1,l100._2,l100._3)

  def parseGpx(path:Path,requireElevation:Boolean):Result[Gpx] =
    boundary(s"parse GPX $path") {
      val dbf=DocumentBuilderFactory.newInstance()
      dbf.setNamespaceAware(true)
      Try(dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true))
      Try(dbf.setFeature("http://xml.org/sax/features/external-general-entities",false))
      Try(dbf.setFeature("http://xml.org/sax/features/external-parameter-entities",false))
      Try(dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false))
      val doc=dbf.newDocumentBuilder().parse(path.toFile)
      val segments=doc.getElementsByTagNameNS("*","trkseg")
      val nonEmptySegments=(0 until segments.getLength).flatMap { i =>
        val seg=segments.item(i).asInstanceOf[org.w3c.dom.Element]
        val pts=seg.getElementsByTagNameNS("*","trkpt")
        if pts.getLength>0 then Some(seg) else None
      }.toVector
      if nonEmptySegments.size != 1 then
        throw new IllegalArgumentException(
          s"expected exactly one non-empty <trkseg>, found ${nonEmptySegments.size}"
        )
      val nodes=nonEmptySegments.head.getElementsByTagNameNS("*","trkpt")
      val parsed=Vector.newBuilder[Point]
      var issue:Option[String]=None
      var i=0

      while i < nodes.getLength && issue.isEmpty do
        val element=nodes.item(i).asInstanceOf[org.w3c.dom.Element]
        val lat=element.getAttribute("lat").toDoubleOption
        val lon=element.getAttribute("lon").toDoubleOption
        val eles=element.getElementsByTagNameNS("*","ele")
        val ele =
          if eles.getLength == 0 then
            if requireElevation then None else Some(0.0)
          else eles.item(0).getTextContent.trim.toDoubleOption

        (lat,lon,ele) match
          case (Some(la),Some(lo),Some(el))
              if la.isFinite && lo.isFinite && el.isFinite &&
                la >= -90.0 && la <= 90.0 && lo >= -180.0 && lo <= 180.0 =>
            parsed += Point(la,lo,el)
          case _ =>
            issue=Some(
              if requireElevation && eles.getLength == 0 then s"missing <ele> at point $i"
              else s"invalid/non-finite track point $i"
            )
        i += 1

      (issue,parsed.result())
    }.flatMap {
      case (Some(problem),_) => Left(s"$path: $problem")
      case (None,points) if points.size < 2 => Left(s"$path: GPX must have >=2 track points")
      case (None,points) =>
        val name=nfc(path.getFileName.toString.replaceFirst("(?i)\\.gpx$",""))
        Right(Gpx(name,points))
    }

  def gpxFiles(dir:Path):Result[Vector[Path]] =
    if !Files.isDirectory(dir) then Right(Vector.empty)
    else
      boundary(s"list $dir") {
        val stream=Files.list(dir)
        try
          stream.iterator().asScala
            .filter(p =>
              Files.isRegularFile(p) &&
                p.getFileName.toString.toLowerCase(Locale.ROOT).endsWith(".gpx")
            )
            .toVector
            .sortBy(_.getFileName.toString)
        finally stream.close()
      }

  def sequence[A](xs:Vector[Result[A]]):Result[Vector[A]] =
    xs.foldLeft[Result[Vector[A]]](Right(Vector.empty)) {
      case (Left(problem),_) => Left(problem)
      case (Right(_),Left(problem)) => Left(problem)
      case (Right(acc),Right(value)) => Right(acc :+ value)
    }

  def loadInputs(input:Path):Result[(Vector[Trail],Vector[Gpx],Vector[Gpx])] =
    for
      mandatoryPaths <- gpxFiles(input)
      avoidPaths <- gpxFiles(input.resolve("avoid"))
      realPaths <- gpxFiles(input.resolve("real"))
      _ <-
        if mandatoryPaths.size == 10 then Right(())
        else Left(s"mandatory GPX count ${mandatoryPaths.size}, expected 10")
      _ <-
        if avoidPaths.size == 10 then Right(())
        else Left(s"avoid GPX count ${avoidPaths.size}, expected 10")
      _ <-
        if realPaths.size == 4 then Right(())
        else Left(s"real GPX count ${realPaths.size}, expected 4")
      identities=mandatoryPaths.map(p => nfc(p.getFileName.toString.replaceFirst("(?i)\\.gpx$","")))
      duplicates=identities.groupBy(identity).collect { case (name,items) if items.size > 1 => name }.toVector
      _ <-
        if duplicates.isEmpty then Right(())
        else Left(s"duplicate mandatory identities: ${duplicates.mkString(", ")}")
      mandatory <- sequence(mandatoryPaths.map(parseGpx(_,true)))
      avoids <- sequence(avoidPaths.map(parseGpx(_,false)))
      reals <- sequence(realPaths.map(parseGpx(_,true)))
      trails=mandatory.map { g =>
        Trail(g.name,g.points,demandingMeasurements(g.points),physics(g.points,0.010,Some(TrailDownhillMaxKph)))
      }.sortBy(_.name)
    yield (trails,avoids,reals)


  def minPedalingSpeedMps: Double =
    val diameterM=WheelInch*0.0254
    val circumference=math.Pi*diameterM
    circumference*(FrontTeeth/RearTeeth)*(MinimumCadence/60.0)

  def riderPower(theta: Double, v: Double, crr: Double): Double =
    val fg = TotalMass*G*math.sin(theta) + crr*TotalMass*G*math.cos(theta)
    val fa = 0.5*Rho*CdA*v*v
    math.max(0.0,(fg+fa)*v)/DrivetrainEfficiency

  private case class SegmentRide(speedMps:Double, coasting:Boolean, riderPowerW:Double)

  private def poweredSpeedForTarget(theta:Double, crr:Double):Double =
    val wheelTarget=TargetPower*DrivetrainEfficiency
    val sinTheta=math.sin(theta)
    val cosTheta=math.cos(theta)
    def requiredWheelPower(v:Double):Double =
      val fg=TotalMass*G*sinTheta + crr*TotalMass*G*cosTheta
      val fa=0.5*Rho*CdA*v*v
      (fg+fa)*v
    var lo=0.05
    var hi=20.0
    var k=0
    while k<80 do
      val mid=(lo+hi)/2.0
      if requiredWheelPower(mid)<=wheelTarget then lo=mid else hi=mid
      k+=1
    lo

  private def segmentRide(grade:Double, crr:Double, downhillCapKph:Option[Double]):SegmentRide =
    val theta=math.atan(grade)
    val sinTheta=math.sin(theta)
    val cosTheta=math.cos(theta)
    val capMps=downhillCapKph.map(_/3.6)
    def applyCap(v:Double):Double=capMps.fold(v)(cap=>math.min(v,cap))

    val coastDriveForce = -TotalMass*G*sinTheta - crr*TotalMass*G*cosTheta
    if grade<0.0 && coastDriveForce>0.0 then
      val terminal=math.sqrt((2.0*coastDriveForce)/(Rho*CdA))
      val technicalTrail=downhillCapKph.nonEmpty
      if technicalTrail || terminal >= (2.0/3.6) then
        SegmentRide(applyCap(terminal),coasting=true,riderPowerW=0.0)
      else
        val easy=poweredSpeedForTarget(theta,crr)
        val minPedal=minPedalingSpeedMps
        if easy>=minPedal then SegmentRide(easy,false,TargetPower)
        else SegmentRide(minPedal,false,math.max(TargetPower,riderPower(theta,minPedal,crr)))
    else
      val easy=poweredSpeedForTarget(theta,crr)
      val minPedal=minPedalingSpeedMps
      if easy>=minPedal then SegmentRide(applyCap(easy),false,TargetPower)
      else SegmentRide(applyCap(minPedal),false,math.max(TargetPower,riderPower(theta,minPedal,crr)))

  /**
   * Production rider physics intentionally uses ~30 m elevation chunks.
   * Canonical /height is too noisy for 10 m instantaneous-power decisions;
   * exact 30/100 m wall grades are still evaluated independently by wallMetrics.
   * Downhill transfer chunks coast when gravity can sustain practical speed;
   * mandatory technical GPX additionally uses the canonical 6 km/h descent cap.
   */
  def physics(points: Vector[Point], crr: Double, downhillCapKph:Option[Double]=None): RiderMetrics =
    if points.size<2 then RiderMetrics.Empty
    else
      val raw=mutable.ArrayBuffer.empty[(Double,Double)]
      var anchor=points.head
      var accumulated=0.0
      var i=1
      while i<points.size do
        val current=points(i)
        accumulated += haversine(points(i-1),current)
        val flush=accumulated>=PhysicsGradeWindowM || i==points.size-1
        if flush && accumulated>0.0 then
          raw += ((accumulated,current.ele-anchor.ele))
          anchor=current
          accumulated=0.0
        i+=1

      if raw.size>=2 && raw.last._1<PhysicsGradeWindowM*0.5 then
        val tail=raw.remove(raw.size-1)
        val prev=raw.remove(raw.size-1)
        raw += ((prev._1+tail._1,prev._2+tail._2))

      raw.foldLeft(RiderMetrics.Empty) { case(acc,(distanceM,deltaM)) =>
        val grade=if distanceM>0 then deltaM/distanceM else 0.0
        val ride=segmentRide(grade,crr,downhillCapKph)
        val sec=distanceM/math.max(ride.speedMps,0.05)
        val p=if ride.coasting then 0.0 else ride.riderPowerW
        val spike=if p>120.0 then math.pow((p-120.0)/40.0,2)*sec else 0.0
        acc.concat(
          RiderMetrics(
            sec,
            if p>120.0 then sec else 0.0,
            if p>140.0 then sec else 0.0,
            if p>160.0 then sec else 0.0,
            Streak.constant(sec,p>120.0),
            Streak.constant(sec,p>140.0),
            Streak.constant(sec,p>180.0),
            spike
          )
        )
      }

  def ascent(points: Vector[Point]): Double = points.sliding(2).map{case Vector(a,b)=>math.max(0.0,b.ele-a.ele);case _=>0.0}.sum

  def maxPointGap(points:Vector[Point]):Double =
    points.sliding(2).map {
      case Vector(a,b) => haversine(a,b)
      case _ => 0.0
    }.foldLeft(0.0)(math.max)

  def surfaceCrr(surfaceRaw: String, unpaved: Boolean = false): Double =
    val s=surfaceRaw.trim.toLowerCase(Locale.ROOT).replace('-','_').replace(' ','_')
    s match
      case "paved_smooth" | "smooth_paved" => 0.006
      case "paved" => 0.007
      case "compacted" => 0.010
      case "gravel" => 0.013
      case "dirt" | "earth" => 0.016
      case "mud" => 0.025
      case "sand" => 0.030
      case "impassable" => 0.050
      case "unpaved" | "unknown_unpaved" => 0.014
      case _ if unpaved => 0.014
      case _ => 0.010


  def lengthWeightedCrr(edges: Vector[EdgeAttr]): Double =
    val usable=edges.filter(e => e.lengthM.isFinite && e.lengthM>0.0)
    val total=usable.map(_.lengthM).sum
    if total<=0 then 0.010 else usable.map(e=>e.lengthM*surfaceCrr(e.surface,e.unpaved)).sum/total

  def wallMetrics(points: Vector[Point], rider: RiderMetrics): WallMetrics = WallMetrics(maxExactUphillGrade(points,30),maxExactUphillGrade(points,100),rider.streak180.localMax)

  def cycleLaneKind(cycle:String):String =
    Option(cycle).getOrElse("").trim.toLowerCase(Locale.ROOT) match
      case "" | "no" | "none" | "unknown" | "false" => "none"
      case other => other

  def protectedCycle(cycle: String): Boolean =
    !Set("none","shared").contains(cycleLaneKind(cycle))

  case class EdgeRun(edges: Vector[EdgeAttr], begin: Int, end: Int)

  def edgeRuns(edges: Vector[EdgeAttr], predicate: EdgeAttr => Boolean): Vector[EdgeRun] =
    val out = mutable.ArrayBuffer.empty[EdgeRun]
    var current = mutable.ArrayBuffer.empty[EdgeAttr]

    def flush(): Unit =
      if current.nonEmpty then
        out += EdgeRun(current.toVector, current.head.begin, current.last.end)
        current.clear()

    edges.foreach { e =>
      if e.lengthM > 0 && predicate(e) then
        if current.nonEmpty && e.begin > current.last.end + 1 then flush()
        current += e
      else
        flush()
    }
    flush()
    out.toVector

  def runGeometry(geometry: Vector[Point], run: EdgeRun): Vector[Point] =
    if geometry.isEmpty || run.begin < 0 || run.end < run.begin || run.end >= geometry.size then Vector.empty
    else geometry.slice(run.begin, run.end + 1)

  def modeledRunSeconds(geometry: Vector[Point], run: EdgeRun): Double =
    val points = runGeometry(geometry, run)
    if points.size < 2 then Double.PositiveInfinity
    else physics(points, lengthWeightedCrr(run.edges)).duration

  /**
    * Finite unprotected-primary exposure is a rider-quality cost, not a hard
    * graph deletion. It remains fully measured by roadStress, so calmer
    * alternatives dominate it when otherwise competitive. Motorway/trunk,
    * steps/ferry/rail/impassable remain hard-invalid; an unmodelable primary
    * duration still fails closed.
    */
  def unprotectedPrimaryRuns(edges: Vector[EdgeAttr], geometry: Vector[Point]): Vector[(EdgeRun,Double)] =
    edgeRuns(
      edges,
      e => e.roadClass.equalsIgnoreCase("primary") && !protectedCycle(e.cycleLane)
    ).map(run => run -> modeledRunSeconds(geometry, run))

  def safetyReasons(edges: Vector[EdgeAttr], geometry: Vector[Point]): Vector[String] =
    val reasons=mutable.ArrayBuffer.empty[String]
    edges.foreach { e =>
      val rc=e.roadClass.toLowerCase(Locale.ROOT); val use=e.use.toLowerCase(Locale.ROOT); val surf=e.surface.toLowerCase(Locale.ROOT)
      if !e.lengthM.isFinite || e.lengthM < 0.0 then reasons += "invalid-edge-length"
      else if e.lengthM > 0 then
        if rc.isEmpty then reasons += "missing-road-class"
        if use.isEmpty then reasons += "missing-use"
        if surf.isEmpty then reasons += "missing-surface"
        if e.begin < 0 || e.end < e.begin || e.begin >= geometry.size || e.end >= geometry.size then reasons += "missing-edge-correspondence"
        if rc=="motorway" then reasons += "motorway"
        if rc=="trunk" then reasons += "trunk"
        if Set("steps","ferry","rail-ferry","rail_ferry","rail").contains(use) then reasons += use
        if surf=="impassable" then reasons += "impassable"
    }

    // Unknown modeled duration still fails closed because the road exposure
    // cannot be established.  A finite >=120 s run is evidence only in FIX31.
    unprotectedPrimaryRuns(edges,geometry).foreach { case(_,sec) =>
      if !sec.isFinite then reasons += "primary-unknown-time"
    }

    reasons.toVector.distinct

  def modeledEdgeSeconds(
      edges: Vector[EdgeAttr],
      geometry: Vector[Point],
      predicate: EdgeAttr => Boolean
  ): Double =
    edgeRuns(edges, predicate).map(modeledRunSeconds(geometry, _)).sum

  def roadStress(edges: Vector[EdgeAttr], geometry: Vector[Point]): Double =
    modeledEdgeSeconds(edges, geometry, e => Set("motorway", "trunk").contains(e.roadClass.toLowerCase(Locale.ROOT))) +
      modeledEdgeSeconds(edges, geometry, e => e.roadClass.equalsIgnoreCase("primary") && cycleLaneKind(e.cycleLane)=="shared") +
      modeledEdgeSeconds(edges, geometry, e => e.roadClass.equalsIgnoreCase("primary") && !protectedCycle(e.cycleLane) && cycleLaneKind(e.cycleLane)!="shared") +
      modeledEdgeSeconds(edges, geometry, e => e.roadClass.equalsIgnoreCase("secondary") && cycleLaneKind(e.cycleLane)=="shared") +
      modeledEdgeSeconds(edges, geometry, e => e.roadClass.equalsIgnoreCase("secondary") && !protectedCycle(e.cycleLane) && cycleLaneKind(e.cycleLane)!="shared")


  case class Projection(lat0: Double, lon0: Double):
    private val cos0=math.cos(math.toRadians(lat0))
    def xy(p: Point): XY = XY(EarthR*math.toRadians(p.lon-lon0)*cos0,EarthR*math.toRadians(p.lat-lat0))

  def dot(a:XY,b:XY)=a.x*b.x+a.y*b.y
  def sub(a:XY,b:XY)=XY(a.x-b.x,a.y-b.y)
  def add(a:XY,b:XY)=XY(a.x+b.x,a.y+b.y)
  def mul(a:XY,t:Double)=XY(a.x*t,a.y*t)
  def norm2(a:XY)=dot(a,a)
  def dist(a:XY,b:XY)=math.hypot(a.x-b.x,a.y-b.y)

  def segmentClosestParam(p: XY, a: XY, b: XY): Double =
    val ab=sub(b,a); val d=norm2(ab)
    if d<=1e-12 then 0.0 else math.max(0.0,math.min(1.0,dot(sub(p,a),ab)/d))

  def solveCircleInterval(a:XY,b:XY,c:XY,r:Double): Option[(Double,Double)] =
    val d=sub(b,a); val f=sub(a,c); val A=norm2(d)
    if A<=1e-12 then if dist(a,c)<=r then Some((0,1)) else None
    else
      val B=2*dot(f,d); val C=norm2(f)-r*r; val disc=B*B-4*A*C
      if disc<0 then None
      else
        val q=math.sqrt(math.max(0,disc)); val t1=(-B-q)/(2*A); val t2=(-B+q)/(2*A)
        val lo=math.max(0.0,math.min(t1,t2)); val hi=math.min(1.0,math.max(t1,t2))
        if lo<=hi then Some((lo,hi)) else None

  def clipSegmentToCapsule(a:XY,b:XY,c:XY,d:XY,r:Double): Vector[(Double,Double)] =
    val candidates=mutable.ArrayBuffer.empty[(Double,Double)]
    solveCircleInterval(a,b,c,r).foreach(candidates += _)
    solveCircleInterval(a,b,d,r).foreach(candidates += _)
    val ab=sub(b,a); val cd=sub(d,c); val cd2=norm2(cd)
    if cd2>1e-12 && norm2(ab)>1e-12 then
      val len = math.sqrt(cd2); val ux = cd.x / len; val uy = cd.y / len; val nx = -uy; val ny = ux
      val da=dot(sub(a,c),XY(nx,ny)); val db=dot(ab,XY(nx,ny))
      val ts=mutable.ArrayBuffer(0.0,1.0)
      if math.abs(db)>1e-12 then
        ts += (( r-da)/db); ts += ((-r-da)/db)
      val projA=dot(sub(a,c),XY(ux,uy)); val projB=dot(ab,XY(ux,uy))
      if math.abs(projB)>1e-12 then
        ts += ((0-projA)/projB); ts += ((len-projA)/projB)
      val sorted=ts.filter(t=>t>=0&&t<=1).distinct.sorted
      sorted.sliding(2).foreach { pair =>
        if pair.size == 2 then
          val x = pair(0)
          val y = pair(1)
          if y >= x then
            val m = (x + y) / 2.0
            val p = add(a, mul(ab, m))
            val pr = dot(sub(p, c), XY(ux, uy))
            val perp = math.abs(dot(sub(p, c), XY(nx, ny)))
            if pr >= 0.0 && pr <= len && perp <= r then
              candidates += ((x, y))
      }
    mergeIntervals(candidates.toVector)

  def mergeIntervals(in: Vector[(Double,Double)], eps:Double=1e-9): Vector[(Double,Double)] =
    if in.isEmpty then Vector.empty
    else
      val s=in.map{case(a,b)=>(math.min(a,b),math.max(a,b))}.sortBy(_._1)
      val out=mutable.ArrayBuffer.empty[(Double,Double)]; var cur=s.head
      s.tail.foreach { x =>
        if x._1<=cur._2+eps then cur=(cur._1,math.max(cur._2,x._2))
        else {out+=cur;cur=x}
      }
      out+=cur; out.toVector

  case class PolylineProjection(alongM: Double, lateralM: Double, signedLateralM: Double)

  def projectToPolyline(p: Point, line: Vector[Point], projection: Projection): Option[PolylineProjection] =
    if line.size < 2 then None
    else
      val pxy=projection.xy(p)
      val lxy=line.map(projection.xy)
      val lcum=cumulative(line)
      var bestDist=Double.PositiveInfinity
      var bestAlong=0.0
      var bestSigned=0.0
      var j=0
      while j+1<lxy.size do
        val a=lxy(j); val b=lxy(j+1); val ab=sub(b,a); val ab2=norm2(ab)
        if ab2>1e-12 then
          val t=segmentClosestParam(pxy,a,b)
          val q=add(a,mul(ab,t))
          val dx=pxy.x-q.x; val dy=pxy.y-q.y
          val d=math.hypot(dx,dy)
          val segLen=math.sqrt(ab2)
          val signed=(ab.x*dy-ab.y*dx)/segLen
          val along=lcum(j)+t*(lcum(j+1)-lcum(j))
          if d<bestDist-1e-9 || (math.abs(d-bestDist)<=1e-9 && along<bestAlong) then
            bestDist=d; bestAlong=along; bestSigned=signed
        j+=1
      if bestDist.isFinite then Some(PolylineProjection(bestAlong,bestDist,bestSigned)) else None

  case class CoTravelChunk(meters: Double, midpoint: Point)

  def interpolatePoint(a:Point,b:Point,t0:Double):Point =
    val t=math.max(0.0,math.min(1.0,t0))
    Point(
      a.lat+t*(b.lat-a.lat),
      a.lon+t*(b.lon-a.lon),
      a.ele+t*(b.ele-a.ele)
    )

  def geoBounds(points: Vector[Point]): GeoBounds =
    require(points.nonEmpty, "cannot build bounds for empty geometry")
    GeoBounds(
      points.map(_.lat).min,
      points.map(_.lat).max,
      points.map(_.lon).min,
      points.map(_.lon).max
    )

  /**
    * Conservative cheap rejection before the exact O(routeSegments*corridorSegments)
    * tube matcher.  A false positive is fine; a false negative is not.
    * Dateline-spanning geometry deliberately bypasses longitude rejection.
    */
  def boundsCouldBeWithinTolerance(a: GeoBounds, b: GeoBounds, toleranceM: Double): Boolean =
    val latPadDeg=math.toDegrees(math.max(0.0,toleranceM)/EarthR)
    val latOverlap=a.maxLat >= b.minLat-latPadDeg && a.minLat <= b.maxLat+latPadDeg
    if !latOverlap then false
    else
      val aSpan=a.maxLon-a.minLon
      val bSpan=b.maxLon-b.minLon
      if aSpan>180.0 || bSpan>180.0 then true
      else
        val maxAbsLat=math.min(89.999999,Vector(a.minLat.abs,a.maxLat.abs,b.minLat.abs,b.maxLat.abs).max+latPadDeg)
        val cosLat=math.cos(math.toRadians(maxAbsLat))
        val lonPadDeg=
          if cosLat<=1e-9 then 180.0
          else math.toDegrees(math.max(0.0,toleranceM)/(EarthR*cosLat))
        a.maxLon >= b.minLon-lonPadDeg && a.minLon <= b.maxLon+lonPadDeg

  // Protected-corridor production geometry uses independently checked exact
  // capsule clipping. Historical OLD differential code has been removed after
  // repeated canonical regressions; the synthetic empty-intersection regression
  // remains in the contract tests below.
  /**
    * Exact continuous tube clipping used by production corridor protection.
    *
    * The metric plane is rebuilt for every connector segment.  This keeps the
    * equirectangular approximation local (tens of metres), while each protected
    * segment contributes an exact capsule: finite-width rectangle + endpoint
    * circles.  Capsule intervals are UNIONED; rectangle constraints are
    * INTERSECTED and an empty intersection stays empty.
    *
    * That last property matters: the historical matcher normalized `(lo, hi)`
    * inside a shared `add` helper.  When two rectangle bands were disjoint and
    * produced `lo > hi`, normalizing them silently manufactured a false interval.
    */
  def exactTubeIntervalsForSegment(
      a: Point,
      b: Point,
      protectedLine: Vector[Point],
      radiusM: Double
  ): Vector[(Double,Double)] =
    if protectedLine.size < 2 || radiusM < 0.0 then Vector.empty
    else
      val projection = Projection(
        (a.lat + b.lat) / 2.0,
        (a.lon + b.lon) / 2.0
      )
      val routeA = projection.xy(a)
      val routeB = projection.xy(b)
      val routeMinX = math.min(routeA.x, routeB.x)
      val routeMaxX = math.max(routeA.x, routeB.x)
      val routeMinY = math.min(routeA.y, routeB.y)
      val routeMaxY = math.max(routeA.y, routeB.y)
      val raw = mutable.ArrayBuffer.empty[(Double,Double)]

      var i = 0
      while i + 1 < protectedLine.size do
        val corridorA = projection.xy(protectedLine(i))
        val corridorB = projection.xy(protectedLine(i + 1))
        val couldIntersect =
          routeMaxX >= math.min(corridorA.x, corridorB.x) - radiusM &&
          routeMinX <= math.max(corridorA.x, corridorB.x) + radiusM &&
          routeMaxY >= math.min(corridorA.y, corridorB.y) - radiusM &&
          routeMinY <= math.max(corridorA.y, corridorB.y) + radiusM
        if couldIntersect then
          raw ++= clipSegmentToCapsule(routeA, routeB, corridorA, corridorB, radiusM)
        i += 1

      mergeIntervals(raw.toVector, 1e-10)

  private def localPolylineProjection(p: Point, line: Vector[Point]): Option[PolylineProjection] =
    projectToPolyline(p, line, Projection(p.lat, p.lon))

  /**
    * Qualifying pieces of connector that genuinely co-travel with a protected
    * corridor.  Being inside the 12 m tube is necessary but not sufficient:
    * the clipped piece must also progress mainly ALONG the corridor.  This
    * excludes perpendicular/oblique crossings while preserving reverse travel.
    */
  def coTravelChunks(
      connector: Vector[Point],
      protectedLine: Vector[Point],
      tolerance: Double,
      minAlongFraction: Double = 0.55
  ): Vector[CoTravelChunk] =
    if connector.size < 2 || protectedLine.size < 2 || tolerance < 0.0 then Vector.empty
    else
      val out = Vector.newBuilder[CoTravelChunk]
      var i = 0
      while i + 1 < connector.size do
        val a = connector(i)
        val b = connector(i + 1)
        val segmentM = haversine(a, b)
        if segmentM > 1e-9 then
          exactTubeIntervalsForSegment(a, b, protectedLine, tolerance).foreach { case (lo, hi) =>
            // exactTubeIntervalsForSegment guarantees hi > lo.
            val clippedM = segmentM * (hi - lo)
            if clippedM > 1e-9 then
              val clippedA = interpolatePoint(a, b, lo)
              val clippedB = interpolatePoint(a, b, hi)
              (localPolylineProjection(clippedA, protectedLine), localPolylineProjection(clippedB, protectedLine)) match
                case (Some(pa), Some(pb)) =>
                  val alongDeltaM = math.abs(pb.alongM - pa.alongM)
                  val alongFraction = alongDeltaM / clippedM
                  // Guard against projection jumping to a distant nearby branch.
                  val plausible = alongDeltaM <= clippedM * 1.8 + 5.0
                  val crossingLike =
                    pa.signedLateralM * pb.signedLateralM < 0.0 &&
                    math.abs(pb.signedLateralM - pa.signedLateralM) >= alongDeltaM - 1e-6
                  if !crossingLike && alongFraction >= minAlongFraction && plausible then
                    out += CoTravelChunk(
                      clippedM,
                      interpolatePoint(a, b, (lo + hi) / 2.0)
                    )
                case _ => ()
          }
        i += 1
      out.result()

  def continuousCoTravel(
      connector: Vector[Point],
      protectedLine: Vector[Point],
      tolerance: Double,
      minAlongFraction: Double = 0.55
  ): Double =
    coTravelChunks(connector, protectedLine, tolerance, minAlongFraction).map(_.meters).sum

  def coTravelBlockPoint(
      connector: Vector[Point],
      protectedLine: Vector[Point],
      tolerance: Double,
      from: Point,
      to: Point,
      minAlongFraction: Double = 0.55
  ): Option[Point] =
    coTravelChunks(connector, protectedLine, tolerance, minAlongFraction)
      .maxByOption { chunk =>
        math.min(haversine(chunk.midpoint, from), haversine(chunk.midpoint, to))
      }
      .map(_.midpoint)

  def appendExactDistinctPoints(existing:Vector[Point], additions:Vector[Point]):Vector[Point] =
    additions.foldLeft(existing) { (acc,p) =>
      if acc.exists(q => q.lat==p.lat && q.lon==p.lon) then acc else acc :+ p
    }

  def removeConsecutiveDuplicates(points: Vector[Point]): Vector[Point] =
    points.foldLeft(Vector.empty[Point]) { (acc,p) => if acc.lastOption.exists(q=>q.lat==p.lat && q.lon==p.lon) then acc else acc :+ p }

  def headingNoise(points: Vector[Point]): Double =
    val p=removeConsecutiveDuplicates(points)
    if p.size<3 then 0.0
    else
      val hs=p.sliding(2).map { case Vector(a,b) => math.atan2(math.toRadians(b.lon-a.lon)*math.cos(math.toRadians((a.lat+b.lat)/2)), math.toRadians(b.lat-a.lat)); case _=>0.0 }.toVector
      hs.sliding(2).map { case Vector(a,b) =>
        var d=b-a; while d>math.Pi do d-=2*math.Pi; while d< -math.Pi do d+=2*math.Pi; math.abs(d)
        case _=>0.0
      }.sum

  case class ProjectedSample(s: Double, ele: Double, distance: Double)

  def monotonicProject(samples: Vector[Point], reference: Vector[Point]): Vector[ProjectedSample] =
    val ref=removeConsecutiveDuplicates(reference); val rcum=cumulative(ref); val lat0=ref.map(_.lat).sum/ref.size; val lon0=ref.map(_.lon).sum/ref.size; val pr=Projection(lat0,lon0); val rxy=ref.map(pr.xy)
    var prevS=0.0
    val out=mutable.ArrayBuffer.empty[ProjectedSample]
    samples.foreach { p =>
      val xy=pr.xy(p); var bestD=Double.PositiveInfinity; var bestS=prevS
      var j=0
      while j+1<rxy.size do
        val segStart=rcum(j); val segEnd=rcum(j+1)
        if segEnd+1e-9>=prevS then
          var t=segmentClosestParam(xy,rxy(j),rxy(j+1)); val len=segEnd-segStart
          if len>0 && segStart+t*len<prevS then t=math.max(0.0,math.min(1.0,(prevS-segStart)/len))
          val q=add(rxy(j),mul(sub(rxy(j+1),rxy(j)),t)); val d=dist(xy,q); val s=segStart+t*len
          if d<bestD-1e-9 || (math.abs(d-bestD)<1e-9 && s<bestS) then {bestD=d;bestS=s}
        j+=1
      if bestD<=15.0 then {out += ProjectedSample(bestS,p.ele,bestD); prevS=bestS}
    }
    out.toVector

  def median(xs: Vector[Double]): Double =
    if xs.isEmpty then 0.0 else
      val s=xs.sorted; if s.size%2==1 then s(s.size/2) else (s(s.size/2-1)+s(s.size/2))/2

  def interpSamples(samples: Vector[(Double,Double)], s: Double): Option[Double] =
    if samples.isEmpty || s<samples.head._1 || s>samples.last._1 then None
    else if s==samples.last._1 then Some(samples.last._2)
    else
      var i=0
      while i+1<samples.size && samples(i+1)._1<s do i+=1
      if i+1>=samples.size then Some(samples.last._2)
      else
        val a=samples(i); val b=samples(i+1); val d=b._1-a._1
        Some(if d<=1e-9 then a._2 else a._2+(s-a._1)/d*(b._2-a._2))

  case class EvidenceCorridor(label:String, reference:Vector[Point], candidates:Vector[EvidenceCandidate])

  case class RiddenTrailOccurrence(trailIndex:Int, startSampleIndex:Int, endSampleIndex:Int)
  case class RiddenTransferSegment(label:String, points:Vector[Point])

  def percentile(xs:Vector[Double], p:Double):Double =
    if xs.isEmpty then Double.NaN
    else
      val s=xs.sorted
      val idx=math.max(0,math.min(s.size-1,math.round((s.size-1)*p).toInt))
      s(idx)

  /**
   * Recover mandatory-trail occurrences inside a real ride before deriving
   * transfer evidence. This is the production reference semantics: a phone
   * recording is not itself one generic evidence corridor. Evidence belongs
   * only to the actual mandatory->mandatory transfer that was ridden.
   */
  def riddenTrailOccurrence(ride:Gpx, trailIndex:Int, trails:Vector[Trail]):Option[RiddenTrailOccurrence] =
    val trail=trails(trailIndex)
    val denseTrail=samplePolyline(trail.points,8.0)
    val trailLength=if denseTrail.size>=2 then cumulative(denseTrail).last else 0.0
    if denseTrail.size<2 || trailLength<30.0 || ride.points.size<2 then None
    else
      val projection=Projection(
        denseTrail.map(_.lat).sum/denseTrail.size,
        denseTrail.map(_.lon).sum/denseTrail.size
      )
      val projected=ride.points.map { p =>
        projectToPolyline(p,denseTrail,projection).getOrElse(
          PolylineProjection(0.0,Double.PositiveInfinity,0.0)
        )
      }
      val rideCum=cumulative(ride.points)
      val startCandidates=projected.indices.filter { i =>
        projected(i).lateralM<=18.0 && projected(i).alongM<=35.0
      }
      val endCandidates=projected.indices.filter { i =>
        projected(i).lateralM<=18.0 && projected(i).alongM>=trailLength*0.82
      }
      var best=Option.empty[(Int,Int,Double)]
      startCandidates.take(60).foreach { first =>
        endCandidates.iterator.filter(_>first).take(80).foreach { last =>
          val expectedProgress=projected(last).alongM
          val riddenSpan=rideCum(last)-rideCum(first)
          val spanOk=
            riddenSpan>=math.max(20.0,expectedProgress*0.55) &&
              riddenSpan<=expectedProgress*1.80+100.0
          if spanOk then
            val near=projected.slice(first,last+1).filter(_.lateralM<=18.0)
            if near.size>=5 then
              val alongs=near.map(_.alongM)
              val coverage=(alongs.max-alongs.min)/trailLength
              val nearFraction=near.size.toDouble/math.max(1,last-first+1).toDouble
              val p90Gap=percentile(near.map(_.lateralM),0.90)
              if coverage>=0.80 && nearFraction>=0.45 then
                val score=
                  math.abs(riddenSpan-expectedProgress) +
                    p90Gap*5.0 +
                    (1.0-nearFraction)*100.0 +
                    (1.0-coverage)*20.0
                best match
                  case None => best=Some((first,last,score))
                  case Some((_,_,oldScore)) if score<oldScore => best=Some((first,last,score))
                  case _ => ()
        }
      }
      best.map { case(first,last,_) => RiddenTrailOccurrence(trailIndex,first,last) }

  def riddenTransferSegments(ride:Gpx,trails:Vector[Trail]):Vector[RiddenTransferSegment] =
    val occurrences=trails.indices
      .flatMap(i=>riddenTrailOccurrence(ride,i,trails))
      .sortBy(_.startSampleIndex)
      .foldLeft(Vector.empty[RiddenTrailOccurrence]) { (acc,current) =>
        if acc.lastOption.exists(previous=>current.startSampleIndex<=previous.endSampleIndex) then acc
        else acc :+ current
      }
    occurrences.sliding(2).flatMap {
      case Vector(from,to) if to.startSampleIndex>from.endSampleIndex+2 =>
        Some(
          RiddenTransferSegment(
            s"${trails(from.trailIndex).name} -> ${trails(to.trailIndex).name}",
            ride.points.slice(from.endSampleIndex,to.startSampleIndex+1)
          )
        )
      case _ => None
    }.toVector

  case class CorridorMatch(
      matched:Boolean,
      candidateToRideP90M:Double,
      rideToCandidateP90M:Double,
      lengthRatio:Double,
      startGapM:Double,
      endGapM:Double
  )

  def evidenceCorridorMatch(candidate:Vector[Point], reference:Vector[Point]):CorridorMatch =
    val candidateDense=samplePolyline(candidate,20.0)
    val referenceDense=samplePolyline(reference,20.0)
    val candidateLength=if candidateDense.size>=2 then cumulative(candidateDense).last else 0.0
    val referenceLength=if referenceDense.size>=2 then cumulative(referenceDense).last else 0.0
    val ratio=if referenceLength>1e-9 then candidateLength/referenceLength else Double.PositiveInfinity
    def p90DistanceTo(from:Vector[Point],to:Vector[Point]):Double =
      if from.isEmpty || to.size<2 then Double.PositiveInfinity
      else
        val projection=Projection(to.map(_.lat).sum/to.size,to.map(_.lon).sum/to.size)
        percentile(from.flatMap(p=>projectToPolyline(p,to,projection).map(_.lateralM)),0.90)
    val c2r=p90DistanceTo(candidateDense,referenceDense)
    val r2c=p90DistanceTo(referenceDense,candidateDense)
    val startGap=
      if candidateDense.nonEmpty && referenceDense.nonEmpty then haversine(candidateDense.head,referenceDense.head)
      else Double.PositiveInfinity
    val endGap=
      if candidateDense.nonEmpty && referenceDense.nonEmpty then haversine(candidateDense.last,referenceDense.last)
      else Double.PositiveInfinity
    val matched=
      candidateDense.size>=2 &&
        referenceDense.size>=2 &&
        ratio>=0.75 && ratio<=1.30 &&
        c2r<=18.0 && r2c<=18.0 &&
        startGap<=70.0 && endGap<=70.0
    CorridorMatch(matched,c2r,r2c,ratio,startGap,endGap)

  /**
   * Safety-active real-ride evidence is derived only from transfer labels that
   * appear in at least two recordings. Recordings that do not contain two
   * recognizable mandatory occurrences remain validated inputs but do not
   * contaminate unrelated connectors.
   */
  def buildRealRideEvidence(reals:Vector[Gpx],trails:Vector[Trail]):Result[Vector[EvidenceCorridor]] =
    val labeled=reals.sortBy(_.name).flatMap { ride =>
      riddenTransferSegments(ride,trails).map(segment=>(segment.label,ride.name,segment.points))
    }
    val groups=labeled.groupBy(_._1).toVector.sortBy(_._1)
    val evidence=groups.flatMap { case(label,items0) =>
      val items=items0.sortBy(_._2)
      if items.size<2 then None
      else
        val a=removeConsecutiveDuplicates(items(0)._3)
        val b=removeConsecutiveDuplicates(items(1)._3)
        if a.size<2 || b.size<2 then None
        else
          val (ref,other)=if headingNoise(a)<=headingNoise(b) then (a,b) else (b,a)
          val proj=monotonicProject(other,ref).sortBy(_.s)
          if proj.size<2 then None
          else
            val rcum=cumulative(ref)
            val pairedRef=proj.map(x=>(x.s,interpolate(ref,rcum,x.s).ele))
            val rawOther=proj.map(x=>(x.s,x.ele))
            val bias=median(pairedRef.zip(rawOther).map{case(r,o)=>o._2-r._2})
            val adjusted=rawOther.map{case(s,e)=>(s,e-bias)}
            val refFull=rcum.zip(ref.map(_.ele))
            val commonStart=math.max(refFull.head._1,adjusted.head._1)
            val commonEnd=math.min(refFull.last._1,adjusted.last._1)
            if commonEnd<=commonStart then None
            else
              val bestByWindow=Vector(30.0,100.0).flatMap { window =>
                val margin=if window==30.0 then 45.0 else 60.0
                val lo=math.max(commonStart,margin-window/2.0)
                val hi=math.min(commonEnd-window,rcum.last-margin-window/2.0)
                if hi<lo then None
                else
                  val starts=mutable.TreeSet.empty[Double]
                  val breaks=(refFull.map(_._1)++adjusted.map(_._1)).distinct
                  starts+=lo; starts+=hi
                  breaks.foreach { x =>
                    if x>=lo && x<=hi then starts+=x
                    val y=x-window
                    if y>=lo && y<=hi then starts+=y
                  }
                  val tolerance=if window==30.0 then 5.0 else 4.0
                  starts.toVector.flatMap { s0 =>
                    for
                      r0<-interpSamples(refFull,s0)
                      r1<-interpSamples(refFull,s0+window)
                      o0<-interpSamples(adjusted,s0)
                      o1<-interpSamples(adjusted,s0+window)
                      g1=100.0*(r1-r0)/window
                      g2=100.0*(o1-o0)/window
                      if g1>0.0 && g2>0.0 && math.abs(g1-g2)<=tolerance
                    yield EvidenceCandidate(label,window,s0,g1,g2,math.min(g1,g2))
                  }.maxByOption(_.commonPct)
              }
              if bestByWindow.nonEmpty then Some(EvidenceCorridor(label,ref,bestByWindow))
              else None
    }
    Right(evidence)

  def forwardEvidenceLocalGrade(
      candidate:Vector[Point],
      referenceWindow:Vector[Point],
      windowM:Double
  ):Option[Double] =
    if candidate.size<2 || referenceWindow.size<2 || windowM<=0 then None
    else
      val sampled=samplePolyline(referenceWindow,5.0)
      val projected=monotonicProject(sampled,candidate)
      if sampled.size<2 || projected.size!=sampled.size then None
      else
        val span=projected.last.s-projected.head.s
        val directionOk=
          span>=math.max(10.0,windowM*0.50) &&
            projected.sliding(2).forall {
              case Vector(a,b) => b.s>=a.s-5.0
              case _ => true
            }
        if !directionOk then None
        else
          val ccum=cumulative(candidate)
          val a=interpolate(candidate,ccum,projected.head.s)
          val b=interpolate(candidate,ccum,projected.last.s)
          Some(100.0*math.max(0.0,b.ele-a.ele)/windowM)

  def applyEvidence(
      connectorLabel:String,
      geometry:Vector[Point],
      wall:WallMetrics,
      corridors:Vector[EvidenceCorridor]
  ): (Double,Vector[EvidenceApplication]) =
    val physical=wall.physicalSeverity
    val apps=mutable.ArrayBuffer.empty[EvidenceApplication]
    corridors.filter(_.label==connectorLabel).foreach { c =>
      val global=evidenceCorridorMatch(geometry,c.reference)
      if global.matched then
        val accepted=mutable.ArrayBuffer.empty[EvidenceCandidate]
        c.candidates.foreach { ev =>
          val localEvidenceGeom=slicePolyline(c.reference,ev.s,ev.s+ev.windowM)
          val localOverlap=
            if localEvidenceGeom.size>=2 then continuousCoTravel(geometry,localEvidenceGeom,15.0)
            else 0.0
          val directionMatched=forwardEvidenceLocalGrade(geometry,localEvidenceGeom,ev.windowM).nonEmpty
          val connectorLocalMax=if ev.windowM==30.0 then wall.max30Pct else wall.max100Pct
          val delta=if ev.windowM==30.0 then 4.0 else 3.0
          if localOverlap>=ev.windowM-1.0 &&
              directionMatched &&
              ev.commonPct>=connectorLocalMax+delta
          then accepted += ev
        }
        if accepted.nonEmpty then
          val sev=accepted.map(e=>if e.windowM==30.0 then e.commonPct/27.0 else e.commonPct/20.0).max
          if sev>physical+0.06 then
            apps += EvidenceApplication(
              c.label,
              sev,
              accepted.toVector.map(e=>f"${e.windowM}%.0fm@${e.s}%.1f common=${e.commonPct}%.2f%%")
            )
    }
    val floor=apps.map(_.severity).foldLeft(0.0)(math.max)
    (floor,apps.toVector)

  class ValhallaClient(base:String):
    private val http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build()

    private case class RouteKey(
        fromLat:Long,fromLon:Long,toLat:Long,toLon:Long,
        profile:Profile,avoid:Vector[(Long,Long)]
    )
    private case class TraceKey(profile:Profile,shape:Vector[(Long,Long)])

    private val routeCache=mutable.HashMap.empty[RouteKey,Option[(Vector[Point],Double,String)]]
    private val traceCache=mutable.HashMap.empty[TraceKey,(Vector[Point],Vector[EdgeAttr],String)]
    private val elevationCache=mutable.HashMap.empty[Vector[(Long,Long)],Vector[Double]]
    private var routeRequests=0L
    private var routeCacheHits=0L
    private var traceRequests=0L
    private var traceCacheHits=0L
    private var heightRequests=0L
    private var heightCacheHits=0L
    private var routeFetchNanos=0L
    private var traceFetchNanos=0L
    private var heightFetchNanos=0L

    private def bits(x:Double):Long=java.lang.Double.doubleToLongBits(x)
    private def shapeKey(shape:Vector[Point]):Vector[(Long,Long)] =
      shape.map(p => (bits(p.lat),bits(p.lon)))
    private def elevationKey(shape:Vector[Point]):Vector[(Long,Long)] = shapeKey(shape)

    def cacheStats:String =
      f"routeRequests=$routeRequests routeCacheHits=$routeCacheHits routeFetch=${routeFetchNanos/1e9}%.3fs " +
        f"traceRequests=$traceRequests traceCacheHits=$traceCacheHits traceFetch=${traceFetchNanos/1e9}%.3fs " +
        f"heightRequests=$heightRequests heightCacheHits=$heightCacheHits heightFetch=${heightFetchNanos/1e9}%.3fs"

    private def post(path:String,json:Value):Result[(Int,String)] =
      boundary(s"Valhalla POST $path") {
        val req=HttpRequest.newBuilder(URI.create(base+path))
          .timeout(Duration.ofSeconds(60))
          .header("Content-Type","application/json")
          .header("X-Client-Id",BuildId)
          .POST(HttpRequest.BodyPublishers.ofString(json.render(),StandardCharsets.UTF_8))
          .build()
        val response=http.send(req,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        (response.statusCode(),response.body())
      }

    def status():Result[String] =
      boundary("Valhalla GET /status") {
        val req=HttpRequest.newBuilder(URI.create(base+"/status"))
          .timeout(Duration.ofSeconds(20))
          .GET()
          .build()
        val response=http.send(req,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        (response.statusCode(),response.body())
      }.flatMap { case(code,body) =>
        if code/100 == 2 then Right(body.replaceAll("\\s+"," ").take(1000))
        else Left(s"Valhalla status failure HTTP $code: ${body.take(300)}")
      }

    private def options(p:Profile):Obj =
      Obj(
        "bicycle_type"->"mountain",
        "cycling_speed"->p.speedKph,
        "use_hills"->p.useHills,
        "use_roads"->p.useRoads,
        "avoid_bad_surfaces"->0.50
      )

    def route(
        from:Point,
        to:Point,
        p:Profile,
        avoidLocations:Vector[Point]=Vector.empty
    ):Result[Option[(Vector[Point],Double,String)]] =
      val key=RouteKey(bits(from.lat),bits(from.lon),bits(to.lat),bits(to.lon),p,shapeKey(avoidLocations))
      routeCache.get(key) match
        case Some(value) =>
          routeCacheHits+=1
          Right(value)
        case None =>
          routeRequests+=1
          val fetchStarted=System.nanoTime()
          val fetched=fetchRoute(from,to,p,avoidLocations)
          routeFetchNanos += System.nanoTime()-fetchStarted
          fetched.map { value =>
            routeCache.update(key,value)
            value
          }

    private def fetchRoute(
        from:Point,
        to:Point,
        p:Profile,
        avoidLocations:Vector[Point]
    ):Result[Option[(Vector[Point],Double,String)]] =
      val payload=Obj(
        "locations"->Arr(
          Obj("lat"->from.lat,"lon"->from.lon),
          Obj("lat"->to.lat,"lon"->to.lon)
        ),
        "costing"->"bicycle",
        "costing_options"->Obj("bicycle"->options(p)),
        "directions_type"->"none",
        "units"->"kilometers",
        "shape_format"->"polyline6"
      )
      if avoidLocations.nonEmpty then
        payload.obj("avoid_locations")=Arr(avoidLocations.map(q=>Obj("lat"->q.lat,"lon"->q.lon))*)
      post("/route",payload).flatMap { case(code,body) =>
        if code/100 == 2 then
          boundary("decode Valhalla route JSON")(ujson.read(body)).flatMap { json =>
            boundary("read Valhalla route fields") {
              val trip=json("trip")
              val seconds=trip("summary")("time").num
              val shapes=trip("legs").arr.toVector.map(_("shape").str)
              (seconds,shapes)
            }.flatMap { case(seconds,shapes) =>
              if !seconds.isFinite || seconds < 0.0 then
                Left(s"Valhalla route returned invalid transfer time: $seconds")
              else if shapes.isEmpty then
                Left("Valhalla route returned no leg shapes")
              else
                sequence(shapes.map(decodePolyline6)).flatMap { parts =>
                  val geometry=dedupeBoundary(parts.flatten)
                  val invalidCoordinate=geometry.exists(p =>
                    !p.lat.isFinite || !p.lon.isFinite || p.lat < -90.0 || p.lat > 90.0 || p.lon < -180.0 || p.lon > 180.0
                  )
                  if geometry.size<2 then Left("Valhalla route returned an empty/degenerate geometry")
                  else if invalidCoordinate then Left("Valhalla route returned an invalid coordinate")
                  else Right(Some((geometry,seconds,body)):Option[(Vector[Point],Double,String)])
                }
            }
          }
        else
          val low=body.toLowerCase(Locale.ROOT)
          if code == 400 && (low.contains("no path") || low.contains("no route") || low.contains("442")) then Right(None)
          else Left(s"Valhalla route service failure HTTP $code: ${body.take(500)}")
      }

    def traceAttributes(shape:Vector[Point],p:Profile):Result[(Vector[Point],Vector[EdgeAttr],String)] =
      val key=TraceKey(p,shapeKey(shape))
      traceCache.get(key) match
        case Some(value) =>
          traceCacheHits+=1
          Right(value)
        case None =>
          traceRequests+=1
          val fetchStarted=System.nanoTime()
          val fetched=fetchTraceAttributes(shape,p)
          traceFetchNanos += System.nanoTime()-fetchStarted
          fetched.map { value =>
            traceCache.update(key,value)
            value
          }

    private def fetchTraceAttributes(shape:Vector[Point],p:Profile):Result[(Vector[Point],Vector[EdgeAttr],String)] =
      val payload=Obj(
        "shape"->Arr(shape.map(q=>Obj("lat"->q.lat,"lon"->q.lon))*),
        "costing"->"bicycle",
        "costing_options"->Obj("bicycle"->options(p)),
        "shape_match"->"edge_walk",
        "shape_format"->"polyline6",
        "directions_options"->Obj("units"->"kilometers"),
        "filters"->Obj(
          "action"->"include",
          "attributes"->Arr(
            "shape","edge.id","edge.length","edge.speed","edge.road_class",
            "edge.begin_shape_index","edge.end_shape_index","edge.use","edge.surface",
            "edge.cycle_lane","edge.unpaved","node.elapsed_time"
          )
        )
      )

      post("/trace_attributes",payload).flatMap { case(code,body) =>
        if code/100 != 2 then Left(s"Valhalla trace_attributes failure HTTP $code: ${body.take(500)}")
        else
          boundary("decode trace_attributes JSON")(ujson.read(body)).flatMap { json =>
            val shapeResult:Result[Vector[Point]] =
              json.obj.get("shape") match
                case Some(Str(encoded)) => decodePolyline6(encoded)
                case _ => Right(shape)

            val edgeValues=json.obj.get("edges") match
              case Some(a:Arr) => Right(a.arr.toVector)
              case _ => Left("Valhalla trace_attributes response missing edges")

            for
              traced <- shapeResult
              rawEdges <- edgeValues
            yield
              var previousElapsed=0.0
              val edges=rawEdges.zipWithIndex.map { case(edge,idx) =>
                def str(k:String)=edge.obj.get(k).collect{case Str(s)=>s}.getOrElse("")
                def num(k:String,d:Double=0.0)=edge.obj.get(k).collect{case Num(n)=>n}.getOrElse(d)
                def int(k:String,d:Int = -1)=edge.obj.get(k).collect{case Num(n)=>n.toInt}.getOrElse(d)
                def bool(k:String,d:Boolean=false)=edge.obj.get(k).collect{case Bool(b)=>b}.getOrElse(d)

                val begin=int("begin_shape_index")
                val end=int("end_shape_index")
                val elapsed=edge.obj.get("end_node")
                  .collect{case o:Obj=>o.obj.get("elapsed_time").collect{case Num(n)=>n}}
                  .flatten
                val riding=elapsed match
                  case Some(t) if t.isFinite && t >= previousElapsed =>
                    val delta=t-previousElapsed
                    previousElapsed=t
                    delta
                  case _ => Double.NaN

                EdgeAttr(
                  str("id") match
                    case "" => s"edge-$idx"
                    case value => value,
                  begin,end,
                  num("length")*1000.0,
                  num("speed"),
                  str("road_class"),
                  str("use"),
                  str("surface"),
                  str("cycle_lane"),
                  bool("unpaved"),
                  riding
                )
              }.toVector
              (traced,edges,body)
          }
      }

    private def fetchElevations(shape:Vector[Point]):Result[Vector[Double]] =
      val payload=Obj(
        "shape"->Arr(shape.map(q=>Obj("lat"->q.lat,"lon"->q.lon))*),
        "range"->false
      )

      post("/height",payload).flatMap { case(code,body) =>
        if code/100 != 2 then Left(s"Valhalla height service failure HTTP $code: ${body.take(500)}")
        else
          boundary("decode Valhalla height JSON")(ujson.read(body)).flatMap { json =>
            json.obj.get("height") match
              case Some(a:Arr) =>
                val values=a.arr.toVector.map {
                  case Num(n) => Right(n)
                  case arr:Arr if arr.arr.nonEmpty =>
                    arr.arr.last match
                      case Num(n) => Right(n)
                      case other => Left(s"unexpected height element: $other")
                  case other => Left(s"unexpected height element: $other")
                }
                sequence(values).flatMap { elevations =>
                  if elevations.size != shape.size then
                    Left(s"invalid elevation response: ${elevations.size} for ${shape.size} shape points")
                  else if elevations.exists(v => !v.isFinite) then
                    Left("invalid elevation response: non-finite value")
                  else Right(elevations)
                }
              case _ => Left("Valhalla height response missing height")
          }
      }

    def elevations(shape:Vector[Point]):Result[Vector[Double]] =
      val key=elevationKey(shape)
      elevationCache.get(key) match
        case Some(values) =>
          heightCacheHits+=1
          Right(values)
        case None =>
          heightRequests+=1
          val fetchStarted=System.nanoTime()
          val fetched=fetchElevations(shape)
          heightFetchNanos += System.nanoTime()-fetchStarted
          fetched.map { values =>
            elevationCache.update(key,values)
            values
          }


  def decodePolyline6(s:String):Result[Vector[Point]] =
    val out=mutable.ArrayBuffer.empty[Point]
    var index=0
    var lat=0L
    var lon=0L
    var problem:Option[String]=None

    def nextDelta():Option[Long] =
      var result=0L
      var shift=0
      var keepReading=true
      while keepReading && problem.isEmpty do
        if index >= s.length then
          problem=Some("truncated polyline6")
        else
          val b=s.charAt(index).toInt-63
          index += 1
          result |= ((b & 0x1f).toLong << shift)
          shift += 5
          keepReading=b >= 0x20
      if problem.nonEmpty then None
      else Some(if (result & 1L) != 0 then ~(result >> 1) else result >> 1)

    while index < s.length && problem.isEmpty do
      (nextDelta(),nextDelta()) match
        case (Some(dLat),Some(dLon)) =>
          lat += dLat
          lon += dLon
          out += Point(lat/1e6,lon/1e6,0.0)
        case _ => ()

    problem match
      case Some(msg) => Left(msg)
      case None => Right(out.toVector)


  def dedupeBoundary(points:Vector[Point]):Vector[Point] =
    points.foldLeft(Vector.empty[Point])((a,p)=>if a.lastOption.exists(q=>q.lat==p.lat&&q.lon==p.lon) then a else a:+p)

  def attachElevations(shape:Vector[Point],elev:Vector[Double]):Vector[Point] = shape.zip(elev).map{case(p,e)=>p.copy(ele=e)}

  def generateConnector(
      client:ValhallaClient,
      fromKey:String,
      from:Point,
      toKey:String,
      to:Point,
      profile:Profile,
      protectedCorridors:Vector[ProtectedCorridor],
      evidence:Vector[EvidenceCorridor],
      diag:Diagnostics
  ):Result[Option[Connector]] =
    diag.generated += 1
    if diag.generated == 1 || diag.generated % 50 == 0 then
      appendLiveDebug(
        s"connector-progress generated=${diag.generated} noRoute=${diag.noRoute} accepted-so-far=${diag.acceptedVariants} rejects=${diag.hardRejects.values.sum} safetyReroutes=${diag.safetyReroutes}"
      )

    def overlaps(shape:Vector[Point]):Vector[(ProtectedCorridor,Double)] =
      val safetyShape=corridorSafetyGeometry(shape)
      val shapeBounds=geoBounds(safetyShape)
      protectedCorridors.flatMap { c =>
        if boundsCouldBeWithinTolerance(shapeBounds,c.bounds,AvoidToleranceM) then
          Some(c -> continuousCoTravel(safetyShape,c.points,AvoidToleranceM))
        else None
      }

    val maxSafetyReroutes=math.max(
      64,
      protectedCorridors.map(c => math.max(1,c.points.size-1)).sum
    )
    var avoidLocations=Vector.empty[Point]
    var reroutes=0
    var lastOverlap=""

    def addRouteDerivedBlockers(
        shape:Vector[Point],
        bad:Vector[(ProtectedCorridor,Double)]
    ):Boolean =
      val safetyShape=corridorSafetyGeometry(shape)
      lastOverlap=bad.map { case(c,m) => f"${c.label}=$m%.1fm" }.mkString(", ")
      val detailed=bad.map { case(c,_) =>
        (c,coTravelBlockPoint(safetyShape,c.points,AvoidToleranceM,from,to))
      }

      val derived=detailed.flatMap { case(c,p) => p.map(c -> _) }
      if derived.size < bad.size then
        diag.reject("protected-corridor-blocker-unavailable")
        false
      else
        val accumulated=appendExactDistinctPoints(avoidLocations,derived.map(_._2))
        val added=accumulated.size-avoidLocations.size
        if added<=0 then
          diag.reject("protected-corridor-no-progress")
          false
        else
          avoidLocations=accumulated
          diag.safetyReroutes += 1
          derived.foreach { case(c,_) =>
            diag.safetyRerouteCorridors(c.label)=diag.safetyRerouteCorridors(c.label)+1
          }
          true

    while reroutes <= maxSafetyReroutes do
      client.route(from,to,profile,avoidLocations) match
        case Left(problem) =>
          // Transport/service failures are not legitimate no-route outcomes.
          return Left(problem)

        case Right(None) =>
          diag.noRoute += 1
          return Right(None)

        case Right(Some((routeGeometry,rawSeconds,_))) =>
          val routeBad=overlaps(routeGeometry).filter(_._2 > AvoidToleranceM)
          if routeBad.nonEmpty then
            if !addRouteDerivedBlockers(routeGeometry,routeBad) then
              diag.safetyBlockedProfiles += 1
              return Right(None)
            reroutes += 1
          else
            client.traceAttributes(routeGeometry,profile) match
              case Left(problem) =>
                // Exact edge_walk is safety evidence; inability to obtain it is fail-closed.
                return Left(s"exact edge_walk unavailable for $fromKey->$toKey ${profile.id}: $problem")

              case Right((traced,edges,_)) =>
                val badCorrespondence=edges.exists(
                  e => e.begin < 0 || e.end < e.begin || e.begin >= traced.size || e.end >= traced.size
                )

                if traced.size < 2 then
                  diag.reject("missing-trace-shape")
                  diag.safetyBlockedProfiles += 1
                  return Right(None)
                else if edges.isEmpty then
                  diag.reject("missing-trace-edges")
                  diag.safetyBlockedProfiles += 1
                  return Right(None)
                else if badCorrespondence then
                  diag.reject("invalid-edge-shape-correspondence")
                  diag.safetyBlockedProfiles += 1
                  return Right(None)
                else
                  // Reference corridor authority is the routed /route shape, not
                  // trace_attributes' matched edge_walk shape. The latter is retained
                  // exclusively for road/edge safety because its shape can differ
                  // slightly from the route polyline at graph snapping boundaries.
                  //
                  // Re-routing on trace-shape corridor overlap was introduced by the
                  // greenfield rewrite and changes the connector graph relative to the
                  // validated canonical planner. Hard protected-corridor enforcement is
                  // still performed on routeGeometry before scoring and again on the
                  // dense reconstruction geometry below.

                  // Wall/physics/reconstruction need a dense elevation profile.
                  // Edge begin/end indices, however, belong to the exact edge_walk
                  // trace shape. Keep the two geometries separate instead of either
                  // (a) running wall physics on a sparse trace shape or (b) resampling
                  // trace geometry and silently invalidating its edge indices.
                  val processedRoute=resampleConnectorPhysics(routeGeometry,10.0)
                  client.elevations(processedRoute) match
                      case Left(problem) =>
                        return Left(s"route height unavailable for $fromKey->$toKey ${profile.id}: $problem")

                      case Right(routeElevations) =>
                        client.elevations(traced) match
                          case Left(problem) =>
                            // Trace elevation participates in modeled road-run duration.
                            return Left(s"trace height unavailable for $fromKey->$toKey ${profile.id}: $problem")

                          case Right(traceElevations) =>
                            val geometry=attachElevations(processedRoute,routeElevations)
                            val traceGeometry=attachElevations(traced,traceElevations)
                            val rejects=safetyReasons(edges,traceGeometry)
                            if rejects.nonEmpty then
                              rejects.foreach(diag.reject)
                              diag.safetyBlockedProfiles += 1
                              return Right(None)

                            val crr=lengthWeightedCrr(edges)
                            val rider=physics(geometry,crr)
                            val wall=wallMetrics(geometry,rider)

                            if wall.max30Pct >= 27.0 then
                              diag.reject("max30>=27")
                              diag.safetyBlockedProfiles += 1
                              return Right(None)
                            else if wall.max100Pct >= 20.0 then
                              diag.reject("max100>=20")
                              diag.safetyBlockedProfiles += 1
                              return Right(None)
                            else if wall.above180Seconds >= 90.0 then
                              diag.reject("above180>=90s")
                              diag.safetyBlockedProfiles += 1
                              return Right(None)

                            val (floor,apps)=applyEvidence(s"$fromKey -> $toKey",geometry,wall,evidence)
                            if apps.nonEmpty then diag.evidenceApplied += 1
                            val physical=wall.physicalSeverity
                            val effective=math.max(physical,floor)

                            // Production reference semantics: real-ride evidence
                            // is safety-active. An effective wall at or above the
                            // hard envelope is forbidden, not a new >1 wall class.
                            if effective >= 1.0 - 1e-9 then
                              diag.reject("effective-wall>=1")
                              diag.safetyBlockedProfiles += 1
                              return Right(None)

                            val stress=roadStress(edges,traceGeometry)

                            if !stress.isFinite then
                              diag.reject("nonfinite-road-time")
                              diag.safetyBlockedProfiles += 1
                              return Right(None)

                            val finalOverlaps=overlaps(geometry)
                            val hardFinal=finalOverlaps.filter(_._2 > AvoidToleranceM)
                            if hardFinal.nonEmpty then
                              // Resampling/elevation do not move the routed horizontal line.
                              hardFinal.foreach { case(c,_) => diag.reject("protected-final:"+c.label) }
                              diag.safetyBlockedProfiles += 1
                              return Right(None)

                            val warnings=finalOverlaps.collect {
                              case(c,m) if m>0.0 && m<=AvoidToleranceM => c.label -> m
                            }
                            val provenance=
                              Vector(
                                s"route profile=${profile.id}",
                                s"route-derived blockers=${avoidLocations.size}",
                                s"processed-route-spacing=10m-segmentwise-reference",
                                s"physicalWall=$physical",
                                s"evidenceFloor=$floor"
                              ) ++
                                apps.flatMap(
                                  a => Vector(s"real-ride ${a.corridor} severity=${a.severity}") ++ a.details
                                )


                            diag.acceptedVariants += 1
                            return Right(
                              Some(
                                Connector(
                                  s"$fromKey->$toKey@${profile.id}",
                                  fromKey,toKey,profile,geometry,traceGeometry,rawSeconds,edges,
                                  stress,ascent(geometry),crr,rider,wall,
                                  physical,floor,effective,apps,warnings,provenance
                                )
                              )
                            )

    diag.reject("protected-corridor-reroute-cap")
    diag.safetyBlockedProfiles += 1
    appendLiveDebug(
      s"connector-safety-cap from=$fromKey to=$toKey profile=${profile.id} cap=$maxSafetyReroutes last=$lastOverlap"
    )
    Right(None)



  // The canonical production planner optimizes transfer time using the
  // rider/terrain physics model. Valhalla trip.summary.time is retained only
  // as routing provenance/diagnostic data.
  def connectorTransferSeconds(c:Connector):Double = c.rider.duration

  def connectorDominates(a:Connector,b:Connector):Boolean =
    val sameAscent=java.lang.Double.doubleToLongBits(a.ascentM)==java.lang.Double.doubleToLongBits(b.ascentM)
    if !sameAscent then false
    else
      val ar=a.rider; val br=b.rider
      val noWorse=connectorTransferSeconds(a)<=connectorTransferSeconds(b) && a.roadStressSeconds<=b.roadStressSeconds && a.effectiveWall<=b.effectiveWall && ar.t120<=br.t120 && ar.t140<=br.t140 && ar.t160<=br.t160 && ar.candHard<=br.candHard && ar.spike<=br.spike && streakNoWorse(ar.streak120,br.streak120) && streakNoWorse(ar.streak140,br.streak140)
      val strict=connectorTransferSeconds(a)<connectorTransferSeconds(b) || a.roadStressSeconds<b.roadStressSeconds || a.effectiveWall<b.effectiveWall || ar.t120<br.t120 || ar.t140<br.t140 || ar.t160<br.t160 || ar.spike<br.spike || streakStrict(ar.streak120,br.streak120) || streakStrict(ar.streak140,br.streak140)
      noWorse&&strict

  def streakNoWorse(a:Streak,b:Streak):Boolean = a.prefix<=b.prefix && a.suffix<=b.suffix && a.localMax<=b.localMax && (!a.allAbove || b.allAbove) && a.duration<=b.duration
  def streakStrict(a:Streak,b:Streak):Boolean = a.prefix<b.prefix || a.suffix<b.suffix || a.localMax<b.localMax || (!a.allAbove&&b.allAbove) || a.duration<b.duration

  case class EdgeSemanticKey(
      id:String,
      begin:Int,
      end:Int,
      lengthBits:Long,
      speedBits:Long,
      roadClass:String,
      use:String,
      surface:String,
      cycleLane:String,
      unpaved:Boolean,
      ridingSecondsBits:Long
  )
  case class ConnectorSemanticKey(
      rawSecondsBits:Long,
      geometryBits:Vector[(Long,Long,Long)],
      traceGeometryBits:Vector[(Long,Long,Long)],
      edges:Vector[EdgeSemanticKey]
  )

  def doubleBits(x:Double):Long=java.lang.Double.doubleToLongBits(x)

  def semanticKey(c:Connector):ConnectorSemanticKey =
    ConnectorSemanticKey(
      rawSecondsBits=doubleBits(c.rawSeconds),
      geometryBits=c.geometry.map(p => (doubleBits(p.lat),doubleBits(p.lon),doubleBits(p.ele))),
      traceGeometryBits=c.traceGeometry.map(p => (doubleBits(p.lat),doubleBits(p.lon),doubleBits(p.ele))),
      edges=c.edges.map { e =>
        EdgeSemanticKey(
          e.id,e.begin,e.end,doubleBits(e.lengthM),doubleBits(e.speedKph),
          e.roadClass,e.use,e.surface,e.cycleLane,e.unpaved,doubleBits(e.ridingSeconds)
        )
      }
    )

  def pruneConnectors(in:Vector[Connector]):Vector[Connector] =
    // Semantic dedupe is deliberately bit-exact.  Near-identical geometry is
    // not equality and must reach exact Pareto dominance instead of being
    // rounded away here.
    val dedup=in.groupBy(semanticKey).values.map(_.minBy(_.profile.id)).toVector.sortBy(_.id)
    dedup.filterNot(b=>dedup.exists(a=>a.id!=b.id && connectorDominates(a,b)))

  def buildGraph(
      client:ValhallaClient,
      trails:Vector[Trail],
      avoids:Vector[Gpx],
      evidence:Vector[EvidenceCorridor],
      diag:Diagnostics
  ):Result[Map[(String,String),Vector[Connector]]] =
    case class Task(fromKey:String,from:Point,toKey:String,to:Point,profile:Profile)

    // Mandatory technical GPXs and explicit avoid GPXs are both protected
    // from connector reuse.  Only mandatory GPXs participate in the solver.
    val protectedCorridors=
      trails.map(t => ProtectedCorridor(t.name,t.points,"mandatory")) ++
        avoids.map(a => ProtectedCorridor(a.name,a.points,"avoid"))

    val tasks=
      (
        trails.flatMap(t => Profiles.map(p => Task("START",Start,t.name,t.points.head,p))) ++
        trails.flatMap { fromTrail =>
          trails.filterNot(_.name == fromTrail.name).flatMap { toTrail =>
            Profiles.map(p => Task(fromTrail.name,fromTrail.points.last,toTrail.name,toTrail.points.head,p))
          }
        } ++
        trails.flatMap { trail =>
          Profiles.flatMap { p =>
            Vector(
              Task(trail.name,trail.points.last,"FINISH_LOOP",LoopFinish,p),
              Task(trail.name,trail.points.last,"FINISH_P2P",P2PFinish,p)
            )
          }
        }
      ).toVector

    val raw=mutable.Map.empty[(String,String),mutable.ArrayBuffer[Connector]]

    tasks.foldLeft[Result[Unit]](Right(())) {
      case (Left(problem),_) => Left(problem)
      case (Right(_),task) =>
        generateConnector(
          client,task.fromKey,task.from,task.toKey,task.to,task.profile,protectedCorridors,evidence,diag
        ).map {
          case Some(connector) =>
            raw.getOrElseUpdate((task.fromKey,task.toKey),mutable.ArrayBuffer.empty) += connector
          case None => ()
        }
    }.map { _ =>
      val pruned=raw.view.mapValues(v => pruneConnectors(v.toVector)).toMap
      diag.retained=pruned.values.map(_.size).sum
      pruned
    }



  def rawDominates(a:RawLabel,b:RawLabel):Boolean =
    a.wall<=b.wall && a.transfer<=b.transfer && (a.wall<b.wall || a.transfer<b.transfer)

  def insertRaw(front:mutable.ArrayBuffer[RawLabel],cand:RawLabel,trails:Vector[Trail]):Unit =
    val equal=front.indexWhere(x=>x.wall==cand.wall&&x.transfer==cand.transfer)
    if equal>=0 then
      if cand.signature(trails)<front(equal).signature(trails) then front(equal)=cand
    else if !front.exists(x=>rawDominates(x,cand)) then
      val kept=front.filterNot(x=>rawDominates(cand,x)); front.clear(); front++=kept; front+=cand

  def rawDp(mode:Mode,trails:Vector[Trail],graph:Map[(String,String),Vector[Connector]]):Vector[RawTerminal] =
    val n=trails.size; val states=mutable.Map.empty[(Int,Int),mutable.ArrayBuffer[RawLabel]]
    trails.indices.foreach { i =>
      if !trails(i).demanding.demanding then
        graph.getOrElse(("START",trails(i).name),Vector.empty).foreach { c =>
          val l=RawLabel(1<<i,i,c.effectiveWall,connectorTransferSeconds(c),c.roadStressSeconds,Vector(i),Vector(c)); insertRaw(states.getOrElseUpdate((l.mask,l.last),mutable.ArrayBuffer.empty),l,trails)
        }
    }
    var bits=1
    while bits<n do
      val snapshot=states.toVector.filter{case((mask,_),_)=>Integer.bitCount(mask)==bits}
      snapshot.foreach { case((mask,last),labels) =>
        trails.indices.filter(i=>(mask&(1<<i))==0).foreach { nxt =>
          labels.foreach { l =>
            graph.getOrElse((trails(last).name,trails(nxt).name),Vector.empty).foreach { c =>
              val nl=RawLabel(mask|(1<<nxt),nxt,math.max(l.wall,c.effectiveWall),l.transfer+connectorTransferSeconds(c),l.road+c.roadStressSeconds,l.order:+nxt,l.connectors:+c)
              insertRaw(states.getOrElseUpdate((nl.mask,nl.last),mutable.ArrayBuffer.empty),nl,trails)
            }
          }
        }
      }
      bits+=1
    val full=(1<<n)-1; val terminals=mutable.ArrayBuffer.empty[RawTerminal]
    states.toVector.filter(_._1._1==full).foreach { case((_,last),labels) => labels.foreach { l =>
      graph.getOrElse((trails(last).name,mode.finishKey),Vector.empty).foreach { c =>
        val wall=math.max(l.wall,c.effectiveWall); val transfer=l.transfer+connectorTransferSeconds(c); val road=l.road+c.roadStressSeconds; val con=l.connectors:+c
        terminals+=RawTerminal(mode,wall,transfer,road,l.order,con,l.signature(trails,Some(mode))+"|"+c.id)
      }
    }}
    paretoRawTerminals(terminals.toVector)

  def paretoRawTerminals(v:Vector[RawTerminal]):Vector[RawTerminal] =
    v.filterNot { b =>
      v.exists { a =>
        a.signature!=b.signature &&
          a.wall<=b.wall &&
          a.transfer<=b.transfer &&
          (a.wall<b.wall || a.transfer<b.transfer)
      }
    }.groupBy(x=>(x.wall,x.transfer))
      .values
      .map(_.minBy(_.signature))
      .toVector
      .sortBy(x=>(x.wall,x.transfer,x.signature))

  def breakpoints(front:Vector[RawTerminal]):Vector[Breakpoint] =
    val walls=front.map(_.wall).filter(_ < 1.0-1e-9).distinct.sorted
    val out=mutable.ArrayBuffer.empty[Breakpoint]
    var previousUseful=Option.empty[RawTerminal]
    walls.foreach { w =>
      val eligible=front.filter(x=>x.wall<=w+1e-12)
      if eligible.nonEmpty then
        val current=eligible.minBy(x=>(x.transfer,x.signature))
        val useful=previousUseful match
          case None => true
          case Some(previous) =>
            val gain=previous.transfer-current.transfer
            val orderChanged=previous.order!=current.order
            gain>=180.0-1e-9 || orderChanged
        if useful then
          out += Breakpoint(current.mode,w,current.transfer,current.road,current.signature)
          previousUseful=Some(current)
    }
    out.toVector

  def rawTerminalDiagnostic(t:RawTerminal,trails:Vector[Trail]):String =
    val order=t.order.map(i=>trails(i).name).mkString("->")
    f"raw-frontier ${t.mode} wall=${t.wall}%.9f transfer=${t.transfer}%.3f road=${t.road}%.3f order=$order"

  def fastestRaw(front:Vector[RawTerminal],ceiling:Double):Option[RawTerminal] =
    val eligible = front.filter(_.wall <= ceiling)
    if eligible.isEmpty then None
    else Some(eligible.minBy(x => (x.transfer,x.signature)))

  case class Assignment(classes:Vector[Double],modes:Vector[Mode],routes:Vector[RawTerminal]):
    def totalTransfer=routes.map(_.transfer).sum
    def totalRoad=routes.map(_.road).sum
    def p2pIndex=modes.indexOf(Mode.P2P)

  def chooseAssignment(classes:Vector[Double],loopFront:Vector[RawTerminal],p2pFront:Vector[RawTerminal]):Option[(Vector[Assignment],Assignment)] =
    val all=(0 until 3).flatMap { p2pIdx =>
      val modes=(0 until 3).map(i=>if i==p2pIdx then Mode.P2P else Mode.LOOP).toVector
      val routeOptions=classes.zip(modes).map { case(c,m) =>
        fastestRaw(if m==Mode.LOOP then loopFront else p2pFront,c)
      }
      if routeOptions.forall(_.nonEmpty) then
        Some(Assignment(classes,modes,routeOptions.flatten))
      else None
    }.toVector
    if all.size != 3 then None
    else Some((all,all.minBy(a=>(a.totalTransfer,a.totalRoad,a.p2pIndex))))

  def hasCompleteOrder(mode:Mode,trails:Vector[Trail],graph:Map[(String,String),Vector[Connector]]):Boolean =
    val n=trails.size
    val reachable=mutable.Set.empty[(Int,Int)]
    trails.indices.foreach { i =>
      if !trails(i).demanding.demanding && graph.getOrElse(("START",trails(i).name),Vector.empty).nonEmpty then
        reachable += ((1 << i,i))
    }
    var bits=1
    while bits<n do
      val current=reachable.toVector.filter(x=>Integer.bitCount(x._1)==bits)
      current.foreach { case(mask,last) =>
        trails.indices.foreach { nxt =>
          if (mask & (1 << nxt)) == 0 && graph.getOrElse((trails(last).name,trails(nxt).name),Vector.empty).nonEmpty then
            reachable += ((mask | (1 << nxt),nxt))
        }
      }
      bits += 1
    val full=(1 << n)-1
    reachable.exists { case(mask,last) =>
      mask==full && graph.getOrElse((trails(last).name,mode.finishKey),Vector.empty).nonEmpty
    }

  def graphDiagnostic(trails:Vector[Trail],graph:Map[(String,String),Vector[Connector]],diag:Diagnostics):String =
    val nonEmpty=graph.count(_._2.nonEmpty)
    val expectedTransitions=trails.size + trails.size*math.max(0,trails.size-1) + 2*trails.size
    val empty=math.max(0,expectedTransitions-nonEmpty)
    val startMissing=trails.filter(t=>graph.getOrElse(("START",t.name),Vector.empty).isEmpty).map(_.name)
    val loopFinishMissing=trails.filter(t=>graph.getOrElse((t.name,"FINISH_LOOP"),Vector.empty).isEmpty).map(_.name)
    val p2pFinishMissing=trails.filter(t=>graph.getOrElse((t.name,"FINISH_P2P"),Vector.empty).isEmpty).map(_.name)
    val deadInbound=trails.filter { t =>
      val fromStart=graph.getOrElse(("START",t.name),Vector.empty).nonEmpty
      val fromMandatory=trails.exists(o => o.name!=t.name && graph.getOrElse((o.name,t.name),Vector.empty).nonEmpty)
      !fromStart && !fromMandatory
    }.map(_.name)
    val deadOutbound=trails.filter { t =>
      val toMandatory=trails.exists(o => o.name!=t.name && graph.getOrElse((t.name,o.name),Vector.empty).nonEmpty)
      val toFinish=graph.getOrElse((t.name,"FINISH_LOOP"),Vector.empty).nonEmpty || graph.getOrElse((t.name,"FINISH_P2P"),Vector.empty).nonEmpty
      !toMandatory && !toFinish
    }.map(_.name)
    val rejectText=diag.hardRejects.toVector.sortBy(-_._2).map{case(k,v)=>s"$k=$v"}.mkString(", ")
    s"graph transitions nonEmpty=$nonEmpty empty=$empty acceptedVariants=${diag.acceptedVariants} retained=${diag.retained} safetyReroutes=${diag.safetyReroutes}; startMissing=${startMissing.mkString("[",",","]")}; loopFinishMissing=${loopFinishMissing.mkString("[",",","]")}; p2pFinishMissing=${p2pFinishMissing.mkString("[",",","]")}; deadInbound=${deadInbound.mkString("[",",","]")}; deadOutbound=${deadOutbound.mkString("[",",","]")}; rejects={$rejectText}"

  def graphNodeDiagnostics(trails:Vector[Trail],graph:Map[(String,String),Vector[Connector]]):Vector[String] =
    trails.map { t =>
      val inbound=trails.filterNot(_.name==t.name).count(o => graph.getOrElse((o.name,t.name),Vector.empty).nonEmpty)
      val outbound=trails.filterNot(_.name==t.name).count(o => graph.getOrElse((t.name,o.name),Vector.empty).nonEmpty)
      val start=graph.getOrElse(("START",t.name),Vector.empty).size
      val loop=graph.getOrElse((t.name,"FINISH_LOOP"),Vector.empty).size
      val p2p=graph.getOrElse((t.name,"FINISH_P2P"),Vector.empty).size
      s"graph-node ${t.name} startVariants=$start inboundTransitions=$inbound outboundTransitions=$outbound loopFinishVariants=$loop p2pFinishVariants=$p2p"
    }

  def warmupAfter(order:Vector[Int],trails:Vector[Trail]):Int =
    val first=order.indexWhere(i=>trails(i).demanding.demanding)
    if first<0 then 0 else if first>=2 then 0 else if first==1 then 1 else Int.MaxValue

  def adjacency(order:Vector[Int],trails:Vector[Trail]):Int = order.sliding(2).count{case Vector(a,b)=>trails(a).demanding.demanding&&trails(b).demanding.demanding;case _=>false}

  type RiderStateKey = (Int,Int,Long,Long,Int,Boolean)

  def riderLabelGroupKey(l:RiderLabel,previousCeiling:Option[Double]):RiderStateKey =
    val pa=l.climb.prevAscent.map(java.lang.Double.doubleToLongBits).getOrElse(Long.MinValue)
    val pd=l.climb.prevDelta.map(java.lang.Double.doubleToLongBits).getOrElse(Long.MinValue)
    val genuine=previousCeiling.exists(pc=>l.requiredWall>pc)
    (l.mask,l.last,pa,pd,l.climb.count,genuine)

  def riderNoWorse(a:RiderLabel,b:RiderLabel):Boolean =
    val ar=a.rider; val br=b.rider
    a.transfer<=b.transfer&&ar.t120<=br.t120&&ar.t140<=br.t140&&ar.t160<=br.t160&&ar.candHard<=br.candHard&&streakNoWorse(ar.streak120,br.streak120)&&streakNoWorse(ar.streak140,br.streak140)&&ar.spike<=br.spike&&a.road<=b.road&&a.climb.maxAscent<=b.climb.maxAscent&&a.climb.upward<=b.climb.upward&&a.climb.roughness<=b.climb.roughness&&a.warmupPenalty<=b.warmupPenalty&&a.demandingAdjacency<=b.demandingAdjacency&&a.requiredWall<=b.requiredWall

  def riderStrict(a:RiderLabel,b:RiderLabel):Boolean =
    val ar=a.rider; val br=b.rider
    a.transfer<b.transfer||ar.t120<br.t120||ar.t140<br.t140||ar.t160<br.t160||ar.spike<br.spike||a.road<b.road||a.climb.maxAscent<b.climb.maxAscent||a.climb.upward<b.climb.upward||a.climb.roughness<b.climb.roughness||a.warmupPenalty<b.warmupPenalty||a.demandingAdjacency<b.demandingAdjacency||a.requiredWall<b.requiredWall||streakStrict(ar.streak120,br.streak120)||streakStrict(ar.streak140,br.streak140)

  case class RiderPerf(
      var insertCalls:Long=0L,
      var dominanceChecks:Long=0L,
      var insertNanos:Long=0L
  )

  /**
    * `front` contains one exact future-continuation state only.  FIX8 mixed all
    * prevAscent/prevDelta groups for the same (mask,last) in one ArrayBuffer and
    * re-scanned unrelated groups on every insertion.  That was exact but could
    * become quadratic in the number of continuation states.
    */
  def insertRiderGroup(
      front:mutable.ArrayBuffer[RiderLabel],
      cand:RiderLabel,
      trails:Vector[Trail],
      perf:RiderPerf
  ):Unit =
    val started=System.nanoTime()
    perf.insertCalls += 1
    def noWorse(a:RiderLabel,b:RiderLabel):Boolean =
      perf.dominanceChecks += 1
      riderNoWorse(a,b)
    def strict(a:RiderLabel,b:RiderLabel):Boolean = riderStrict(a,b)
    if !front.exists(x=>noWorse(x,cand)&&strict(x,cand)) then
      val kept=front.filterNot(x=>noWorse(cand,x)&&strict(cand,x))
      val eq=kept.indexWhere { x =>
        !strict(x, cand) &&
        !strict(cand, x) &&
        noWorse(x, cand) &&
        noWorse(cand, x)
      }
      front.clear(); front++=kept
      if eq<0 then front+=cand
      else
        val existing=front(eq)
        if cand.signature(trails)<existing.signature(trails) then front(eq)=cand
    perf.insertNanos += System.nanoTime()-started

  def partialCanStillUpgrade(l:RiderLabel,baseline:RiderTerminal):Boolean =
    val r=l.rider; val b=baseline.rider
    // Every future contribution is non-negative / monotone for these resources.
    // Once a guard is already violated, no continuation can become an eligible
    // upgrade.  This is admissible pruning, not epsilon/beam/top-K pruning.
    r.candHard < b.candHard &&
      r.streak120.localMax <= b.streak120.localMax &&
      r.streak140.localMax <= b.streak140.localMax &&
      r.spike <= b.spike &&
      l.road <= baseline.road &&
      l.climb.maxAscent <= baseline.climb.maxAscent &&
      l.climb.upward <= baseline.climb.upward &&
      l.climb.roughness <= baseline.climb.roughness &&
      l.warmupPenalty <= baseline.warmupPenalty &&
      l.demandingAdjacency <= baseline.demandingAdjacency

  def terminalIsEligibleUpgrade(t:RiderTerminal,baseline:RiderTerminal,previousCeiling:Option[Double]):Boolean =
    val r=t.rider; val b=baseline.rider
    previousCeiling.forall(pc=>t.requiredWall>pc) &&
      r.candHard < b.candHard &&
      r.streak120.localMax <= b.streak120.localMax &&
      r.streak140.localMax <= b.streak140.localMax &&
      r.spike <= b.spike &&
      t.road <= baseline.road &&
      t.climb.maxAscent <= baseline.climb.maxAscent &&
      t.climb.upward <= baseline.climb.upward &&
      t.climb.roughness <= baseline.climb.roughness &&
      t.warmupPenalty <= baseline.warmupPenalty &&
      t.demandingAdjacency <= baseline.demandingAdjacency

  // Tie-breaker used only after the primary transfer/candHard tradeoff is fixed.
  def productSecondaryKey(c:RiderTerminal) =
    (
      (
        c.road,
        c.rider.streak120.localMax,
        c.rider.streak140.localMax,
        c.rider.spike,
        c.climb.maxAscent,
        c.climb.upward,
        c.climb.roughness
      ),
      c.signature
    )

  // Exact 2-D frontier accumulator.  Invariant: as transfer increases, candHard
  // strictly decreases.  Same-transfer/same-candHard ties keep the better guarded
  // secondary metrics/signature.  This is product selection only and never prunes
  // riderDp states.
  def insertTradeoffFrontier(
      frontier:java.util.TreeMap[java.lang.Double,RiderTerminal],
      candidate:RiderTerminal
  ):Unit =
    val key=java.lang.Double.valueOf(candidate.transfer)
    val existing=frontier.get(key)
    if existing!=null then
      val keepCandidate =
        candidate.rider.candHard < existing.rider.candHard ||
          (candidate.rider.candHard == existing.rider.candHard &&
            Vector(existing,candidate).minBy(productSecondaryKey).signature == candidate.signature)
      if !keepCandidate then return
      frontier.put(key,candidate)
    else
      val lower=frontier.lowerEntry(key)
      if lower!=null && lower.getValue.rider.candHard <= candidate.rider.candHard then return
      frontier.put(key,candidate)

    var higher=frontier.higherEntry(key)
    while higher!=null && higher.getValue.rider.candHard >= candidate.rider.candHard do
      frontier.remove(higher.getKey)
      higher=frontier.higherEntry(key)

  def tradeoffFrontierValues(frontier:java.util.TreeMap[java.lang.Double,RiderTerminal]):Vector[RiderTerminal] =
    frontier.values().asScala.toVector

  case class KneeSelection(
      selected:RiderTerminal,
      transferFirst:RiderTerminal,
      comfortFirst:RiderTerminal,
      score:Double,
      transferFraction:Double,
      comfortFraction:Double,
      frontierSize:Int
  )

  case class LocalElbowSelection(
      selected:RiderTerminal,
      index:Int,
      score:Double
  )

  // Production selector since FIX51. Measures the largest local
  // drop in marginal candHard improvement per extra transfer second. Transfer
  // and candHard are both modeled seconds, so the slope is dimensionless.
  // Only neighboring Pareto segments are used; no far FAST/COMFORT normalization
  // and no fixed transfer horizon are introduced.
  def selectLocalMarginalDrop(front:Vector[RiderTerminal]):Option[LocalElbowSelection] =
    if front.isEmpty then None
    else if front.size<3 then Some(LocalElbowSelection(front.head,0,0.0))
    else
      val scored=(1 until front.size-1).toVector.map { i =>
        val a=front(i-1); val b=front(i); val c=front(i+1)
        val dt1=math.max(1e-12,b.transfer-a.transfer)
        val dt2=math.max(1e-12,c.transfer-b.transfer)
        val gain1=math.max(1e-12,a.rider.candHard-b.rider.candHard)
        val gain2=math.max(1e-12,b.rider.candHard-c.rider.candHard)
        val slope1=gain1/dt1
        val slope2=gain2/dt2
        val drop=math.log(slope1/slope2)
        LocalElbowSelection(b,i,drop)
      }
      Some(scored.minBy(x => (-x.score,x.selected.transfer,x.selected.rider.candHard,productSecondaryKey(x.selected))))


  // TEST-ONLY legacy comparator retained solely by regression #19 to prove the
  // old global-extrema-normalized selector is tail-sensitive.
  def selectParetoKnee(candidates:Vector[RiderTerminal]):Option[KneeSelection] =
    if candidates.isEmpty then None
    else
      val tree=new java.util.TreeMap[java.lang.Double,RiderTerminal]()
      candidates.foreach(c=>insertTradeoffFrontier(tree,c))
      selectParetoKneeFromFrontier(tradeoffFrontierValues(tree))

  def selectParetoKneeFromFrontier(front:Vector[RiderTerminal]):Option[KneeSelection] =
    if front.isEmpty then None
    else
      val tf=front.head
      val cf=front.last
      val dt=cf.transfer-tf.transfer
      val dh=tf.rider.candHard-cf.rider.candHard
      def fractions(c:RiderTerminal):(Double,Double,Double) =
        if dt<=1e-12 || dh<=1e-12 then (0.0,0.0,0.0)
        else
          val tx=math.max(0.0,math.min(1.0,(c.transfer-tf.transfer)/dt))
          val cy=math.max(0.0,math.min(1.0,(tf.rider.candHard-c.rider.candHard)/dh))
          (cy-tx,tx,cy)
      val selected =
        if front.size<=2 || dt<=1e-12 || dh<=1e-12 then tf
        else front.minBy { c =>
          val (score,_,_)=fractions(c)
          (-score,c.transfer,c.rider.candHard,productSecondaryKey(c))
        }
      val (score,tx,cy)=fractions(selected)
      Some(KneeSelection(selected,tf,cf,score,tx,cy,front.size))

  def riderDp(
      mode:Mode,
      ceiling:Double,
      previousCeiling:Option[Double],
      baseline:RiderTerminal,
      debugLabel:String,
      trails:Vector[Trail],
      graph:Map[(String,String),Vector[Connector]]
  ):Vector[RiderTerminal] =
    val n=trails.size
    // Full exact continuation key is the state key.  Dominance never compares
    // labels whose future climb semantics differ.
    val states=mutable.Map.empty[RiderStateKey,mutable.ArrayBuffer[RiderLabel]]
    var generatedLabels=0L
    var baselinePruned=0L
    val perf=RiderPerf()
    val dpStarted=System.nanoTime()
    appendLiveDebug(
      f"rider-search-exact $debugLabel baselineTransfer=${baseline.transfer}%.3f noFixedTimeHorizon=true"
    )

    def insert(l:RiderLabel):Unit =
      generatedLabels+=1
      if partialCanStillUpgrade(l,baseline) then
        val key=riderLabelGroupKey(l,previousCeiling)
        insertRiderGroup(states.getOrElseUpdate(key,mutable.ArrayBuffer.empty),l,trails,perf)
      else baselinePruned+=1

    trails.indices.foreach { i =>
      if !trails(i).demanding.demanding then
        graph.getOrElse(("START",trails(i).name),Vector.empty).filter(_.effectiveWall<=ceiling).foreach { c =>
          val rm=c.rider.concat(trails(i).rider)
          val l=RiderLabel(1<<i,i,connectorTransferSeconds(c),c.roadStressSeconds,c.effectiveWall,rm,ClimbState.Empty.add(c.ascentM,0),0,0,Vector(i),Vector(c))
          insert(l)
        }
    }

    var bits=1
    while bits<n do
      val snapshot=states.toVector
        .filter { case(key,_) => Integer.bitCount(key._1)==bits }
        .sortBy(_._1)
      snapshot.foreach { case(key,labels) =>
        val mask=key._1
        val last=key._2
        trails.indices.filter(i=>(mask&(1<<i))==0).foreach { nxt =>
          labels.foreach { l =>
            graph.getOrElse((trails(last).name,trails(nxt).name),Vector.empty).filter(_.effectiveWall<=ceiling).foreach { c =>
              val newOrder=l.order:+nxt
              val firstDemandBefore=l.order.exists(i=>trails(i).demanding.demanding)
              val warm=if firstDemandBefore then l.warmupPenalty else if trails(nxt).demanding.demanding then (if l.order.size>=2 then 0 else 1) else l.warmupPenalty
              val adj=l.demandingAdjacency+(if trails(last).demanding.demanding&&trails(nxt).demanding.demanding then 1 else 0)
              val nl=RiderLabel(
                mask|(1<<nxt),nxt,
                l.transfer+connectorTransferSeconds(c),
                l.road+c.roadStressSeconds,
                math.max(l.requiredWall,c.effectiveWall),
                l.rider.concat(c.rider).concat(trails(nxt).rider),
                l.climb.add(c.ascentM,l.connectors.size),
                warm,adj,newOrder,l.connectors:+c
              )
              insert(nl)
            }
          }
        }
      }
      bits+=1

    val full=(1<<n)-1
    val fullGroups=states.toVector
      .filter { case(key,_) => key._1==full }
      .sortBy(_._1)
    val terminalStarted=System.nanoTime()
    var terminalCandidatesChecked=0L
    var eligibleCandidates=0L
    val tradeoffFrontier=new java.util.TreeMap[java.lang.Double,RiderTerminal]()

    fullGroups.foreach { case(key,labels) =>
      val last=key._2
      labels.foreach { l =>
        graph.getOrElse((trails(last).name,mode.finishKey),Vector.empty).filter(_.effectiveWall<=ceiling).foreach { c =>
          terminalCandidatesChecked += 1
          val rm=l.rider.concat(c.rider)
          val climb=l.climb.add(c.ascentM,l.connectors.size)
          val wall=math.max(l.requiredWall,c.effectiveWall)
          val con=l.connectors:+c
          val terminal=RiderTerminal(mode,l.transfer+connectorTransferSeconds(c),l.road+c.roadStressSeconds,wall,rm,climb,l.warmupPenalty,l.demandingAdjacency,l.order,con,l.signature(trails)+"|"+c.id)
          if terminalIsEligibleUpgrade(terminal,baseline,previousCeiling) then
            eligibleCandidates += 1
            insertTradeoffFrontier(tradeoffFrontier,terminal)
        }
      }
    }
    appendLiveDebug(
      f"rider-terminal DONE $debugLabel candidates=$terminalCandidatesChecked eligible=$eligibleCandidates frontier=${tradeoffFrontier.size} bestFound=${!tradeoffFrontier.isEmpty} elapsed=${(System.nanoTime()-terminalStarted)/1e9}%.3fs"
    )
    val productFront=tradeoffFrontierValues(tradeoffFrontier)
    val localSelection=selectLocalMarginalDrop(productFront)
    val selectedFront=localSelection.map(_.selected).toVector
    localSelection.foreach { e =>
      appendLiveDebug(
        f"rider-policy-selected $debugLabel policy=local-marginal-drop i=${e.index}%d " +
          f"transfer=${e.selected.transfer}%.1f candHard=${e.selected.rider.candHard}%.1f score=${e.score}%.6f"
      )
    }
    appendLiveDebug(
      f"rider-selector DONE $debugLabel policy=local-marginal-drop eligible=$eligibleCandidates frontier=${tradeoffFrontier.size}%d returned=${selectedFront.size} elapsed=${(System.nanoTime()-terminalStarted)/1e9}%.3fs"
    )
    appendLiveDebug(
      f"rider-dp DONE $debugLabel eligible-terminals=$eligibleCandidates selected=${selectedFront.size} generated=$generatedLabels baselinePruned=$baselinePruned " +
        f"insertCalls=${perf.insertCalls} dominanceChecks=${perf.dominanceChecks} insertSec=${perf.insertNanos/1e9}%.3f totalSec=${(System.nanoTime()-dpStarted)/1e9}%.3f"
    )
    selectedFront

  def evaluateRaw(raw:RawTerminal,trails:Vector[Trail]):RiderTerminal =
    var rm=RiderMetrics.Empty; var climb=ClimbState.Empty
    raw.order.zipWithIndex.foreach { case(idx,k)=>
      rm=rm.concat(raw.connectors(k).rider).concat(trails(idx).rider); climb=climb.add(raw.connectors(k).ascentM,k)
    }
    val finish=raw.connectors.last; rm=rm.concat(finish.rider); climb=climb.add(finish.ascentM,raw.connectors.size-1)
    RiderTerminal(raw.mode,raw.transfer,raw.road,raw.wall,rm,climb,warmupAfter(raw.order,trails),adjacency(raw.order,trails),raw.order,raw.connectors,raw.signature)

  def eligibleUpgrade(c:RiderTerminal,b:RiderTerminal,classIdx:Int,classes:Vector[Double]):Boolean =
    val genuine=if classIdx==0 then c.requiredWall<=classes(0) else c.requiredWall>classes(classIdx-1) && c.requiredWall<=classes(classIdx)
    genuine && c.rider.candHard<b.rider.candHard && c.rider.streak120.localMax<=b.rider.streak120.localMax && c.rider.streak140.localMax<=b.rider.streak140.localMax && c.rider.spike<=b.rider.spike && c.road<=b.road && c.climb.maxAscent<=b.climb.maxAscent && c.climb.upward<=b.climb.upward && c.climb.roughness<=b.climb.roughness && c.warmupPenalty<=b.warmupPenalty && c.demandingAdjacency<=b.demandingAdjacency

  def chooseFinal(front:Vector[RiderTerminal],baseline:RiderTerminal,classIdx:Int,classes:Vector[Double]):RiderTerminal =
    val e=front.filter(c=>eligibleUpgrade(c,baseline,classIdx,classes))
    if e.isEmpty then baseline
    else
      val tree=new java.util.TreeMap[java.lang.Double,RiderTerminal]()
      e.foreach(c=>insertTradeoffFrontier(tree,c))
      val productFront=tradeoffFrontierValues(tree)
      selectLocalMarginalDrop(productFront).map(_.selected).getOrElse(productFront.head)

  def sameHorizontalPoint(a:Point,b:Point):Boolean = a.lat==b.lat && a.lon==b.lon
  def exactPoint(a:Point,b:Point):Boolean = sameHorizontalPoint(a,b) && a.ele==b.ele

  def reconstruct(route:RiderTerminal,trails:Vector[Trail]):Vector[Point] =
    val out=mutable.ArrayBuffer.empty[Point]

    def appendConnector(ps:Vector[Point]):Unit =
      if ps.nonEmpty then
        // When a connector begins exactly at the canonical mandatory endpoint,
        // keep the mandatory point/elevation already present and drop only the
        // duplicate connector boundary point.
        if out.lastOption.exists(q=>sameHorizontalPoint(q,ps.head)) then out++=ps.drop(1)
        else out++=ps

    def appendMandatory(ps:Vector[Point]):Unit =
      if ps.nonEmpty then
        // At connector -> mandatory joins, canonical GPX geometry/elevation wins.
        // This removes only an exact horizontal duplicate stitch point and keeps
        // the complete supplied mandatory sequence unchanged.
        if out.lastOption.exists(q=>sameHorizontalPoint(q,ps.head)) then
          out(out.size-1)=ps.head
          out++=ps.drop(1)
        else out++=ps

    route.order.zipWithIndex.foreach { case(idx,k) =>
      appendConnector(route.connectors(k).geometry)
      appendMandatory(trails(idx).points)
    }
    appendConnector(route.connectors.last.geometry)
    out.toVector
  def countSubsequence(hay:Vector[Point],needle:Vector[Point]):Int =
    if needle.isEmpty||hay.size<needle.size then 0 else (0 to hay.size-needle.size).count(i=>needle.indices.forall(j=>exactPoint(hay(i+j),needle(j))))

  def auditClose(a:Double,b:Double,eps:Double=1e-8):Boolean =
    a.isFinite && b.isFinite && math.abs(a-b)<=eps

  def auditSameStreak(a:Streak,b:Streak):Boolean =
    a.allAbove==b.allAbove &&
      auditClose(a.prefix,b.prefix) && auditClose(a.suffix,b.suffix) &&
      auditClose(a.localMax,b.localMax) && auditClose(a.duration,b.duration)

  def auditSameRider(a:RiderMetrics,b:RiderMetrics):Boolean =
    auditClose(a.duration,b.duration) && auditClose(a.t120,b.t120) &&
      auditClose(a.t140,b.t140) && auditClose(a.t160,b.t160) &&
      auditClose(a.spike,b.spike) && auditSameStreak(a.streak120,b.streak120) &&
      auditSameStreak(a.streak140,b.streak140) && auditSameStreak(a.streak180,b.streak180)

  def auditSameClimb(a:ClimbState,b:ClimbState):Boolean =
    a.count==b.count && auditClose(a.maxAscent,b.maxAscent) &&
      auditClose(a.upward,b.upward) && auditClose(a.roughness,b.roughness) &&
      ((a.prevAscent,b.prevAscent) match
        case (None,None) => true
        case (Some(x),Some(y)) => auditClose(x,y)
        case _ => false) &&
      ((a.prevDelta,b.prevDelta) match
        case (None,None) => true
        case (Some(x),Some(y)) => auditClose(x,y)
        case _ => false)

  /** Independent component-wise rider recomputation used by final audit.
   * Connectors use transfer physics; canonical mandatory trails use their
   * production technical downhill cap. Keeping this in one helper prevents
   * audit/search drift when rider physics evolves.
   */
  def recomputeRouteRider(route:RiderTerminal,trails:Vector[Trail]):RiderMetrics =
    var rm=RiderMetrics.Empty
    route.order.zipWithIndex.foreach { case(trailIndex,k) =>
      val connector=route.connectors(k)
      rm=rm
        .concat(physics(connector.geometry,lengthWeightedCrr(connector.edges)))
        .concat(physics(trails(trailIndex).points,0.010,Some(TrailDownhillMaxKph)))
    }
    rm.concat(physics(route.connectors.last.geometry,lengthWeightedCrr(route.connectors.last.edges)))

  def audit(route:RiderTerminal,ceiling:Double,mode:Mode,trails:Vector[Trail],avoids:Vector[Gpx],evidence:Vector[EvidenceCorridor],finalGeom:Vector[Point]):AuditResult =
    val f=mutable.ArrayBuffer.empty[String]; val w=mutable.ArrayBuffer.empty[String]
    val protectedCorridors=
      trails.map(t => ProtectedCorridor(t.name,t.points,"mandatory")) ++
        avoids.map(a => ProtectedCorridor(a.name,a.points,"avoid"))
    if route.mode!=mode then f += s"endpoint mode mismatch: route=${route.mode}, expected=$mode"
    val unknown=route.order.filter(i=>i<0 || i>=trails.size)
    if unknown.nonEmpty then f += s"unknown mandatory indices: ${unknown.distinct.sorted.mkString(",")}"
    if route.order.size!=trails.size || route.order.distinct.size!=trails.size || route.order.toSet!=trails.indices.toSet then f += "mandatory exactly-once/order-set failure"
    trails.foreach { t =>
      val fresh=demandingMeasurements(t.points)
      if fresh.demanding!=t.demanding.demanding then f += s"${t.name}: demanding classification recomputation mismatch"
    }
    if unknown.isEmpty then
      if route.order.headOption.exists(i=>trails(i).demanding.demanding) then f += "demanding trail first"
      val firstDem=route.order.indexWhere(i=>trails(i).demanding.demanding)
      if firstDem==0 then f += "zero warmups before demanding"
    trails.foreach { t =>
      val fw=countSubsequence(finalGeom,t.points); val rv=countSubsequence(finalGeom,t.points.reverse)
      if fw!=1 then f += s"${t.name}: supplied mandatory sequence count=$fw"
      if rv>0 && t.points!=t.points.reverse then f += s"${t.name}: reversed mandatory sequence present"
    }
    if route.connectors.size!=trails.size+1 then f += s"connector count ${route.connectors.size}, expected ${trails.size+1}"
    else if unknown.isEmpty && route.order.size==trails.size then
      if route.connectors.head.from!="START" then f += "first connector does not start at station role"
      if route.connectors.last.to!=mode.finishKey then f += s"finish connector role ${route.connectors.last.to}, expected ${mode.finishKey}"
      route.order.indices.foreach { k =>
        val trail=trails(route.order(k))
        if route.connectors(k).to!=trail.name then f += s"connector $k does not enter ${trail.name}"
        if k>0 then
          val prev=trails(route.order(k-1))
          if route.connectors(k).from!=prev.name then f += s"connector $k does not leave ${prev.name}"
      }
      if route.connectors.last.from!=trails(route.order.last).name then f += "finish connector does not leave last mandatory"

      val recomputedWarmup=warmupAfter(route.order,trails)
      val recomputedAdjacency=adjacency(route.order,trails)
      if recomputedWarmup==Int.MaxValue then f += "zero warmups before demanding"
      else if route.warmupPenalty!=recomputedWarmup then
        f += s"warmupPenalty mismatch: stored=${route.warmupPenalty}, recomputed=$recomputedWarmup"
      if route.demandingAdjacency!=recomputedAdjacency then
        f += s"demandingAdjacency mismatch: stored=${route.demandingAdjacency}, recomputed=$recomputedAdjacency"

      val recomputedRider=recomputeRouteRider(route,trails)
      var recomputedClimb=ClimbState.Empty
      var recomputedTransfer=0.0
      var recomputedRoad=0.0
      route.order.zipWithIndex.foreach { case(_,k) =>
        val connector=route.connectors(k)
        recomputedClimb=recomputedClimb.add(ascent(connector.geometry),k)
        recomputedTransfer += connectorTransferSeconds(connector)
        recomputedRoad += roadStress(connector.edges,connector.traceGeometry)
      }
      val finishConnector=route.connectors.last
      recomputedClimb=recomputedClimb.add(ascent(finishConnector.geometry),route.connectors.size-1)
      recomputedTransfer += connectorTransferSeconds(finishConnector)
      recomputedRoad += roadStress(finishConnector.edges,finishConnector.traceGeometry)
      if !auditClose(route.transfer,recomputedTransfer) then
        f += f"transfer mismatch: stored=${route.transfer}%.6f recomputed=$recomputedTransfer%.6f"
      if !auditClose(route.road,recomputedRoad) then
        f += f"road mismatch: stored=${route.road}%.6f recomputed=$recomputedRoad%.6f"
      if !auditSameRider(route.rider,recomputedRider) then f += "rider metrics recomputation mismatch"
      if !auditSameClimb(route.climb,recomputedClimb) then f += "climb-shape metrics recomputation mismatch"
    if finalGeom.isEmpty then f += "empty reconstructed route"
    else
      if haversine(finalGeom.head,Start)>5 then f += f"start gap ${haversine(finalGeom.head,Start)}%.1f m >5m"
      if haversine(finalGeom.last,mode.finishPoint)>5 then f += f"finish gap ${haversine(finalGeom.last,mode.finishPoint)}%.1f m >5m"
    val recomputedWalls=mutable.ArrayBuffer.empty[Double]
    route.connectors.foreach { c =>
      val gap=maxPointGap(c.geometry)
      if gap>=FinalGapFailM then f+=f"${c.from}->${c.to}: connector point gap $gap%.1fm >=${FinalGapFailM}%.0fm"
      val sr=safetyReasons(c.edges,c.traceGeometry); sr.foreach(x=>f+=s"${c.from}->${c.to}: $x")
      if c.edges.isEmpty then f += s"${c.from}->${c.to}: missing trace edges"
      if c.edges.exists(e=>e.begin<0||e.end<e.begin||e.begin>=c.traceGeometry.size||e.end>=c.traceGeometry.size) then f += s"${c.from}->${c.to}: invalid edge/geometry correspondence"
      protectedCorridors.foreach { corridor =>
        val ov=continuousCoTravel(c.geometry,corridor.points,AvoidToleranceM)
        if ov>AvoidToleranceM then
          f+=f"${c.from}->${c.to}: protected ${corridor.label} overlap $ov%.1fm"
        else if ov>0 then
          w+=f"${c.from}->${c.to}: protected ${corridor.label} overlap $ov%.1fm"
      }
      val crr=lengthWeightedCrr(c.edges); val rm=physics(c.geometry,crr); val wm=wallMetrics(c.geometry,rm)
      if !auditClose(crr,c.crr) then f+=s"${c.from}->${c.to}: Crr recomputation mismatch"
      if !auditSameRider(rm,c.rider) then f+=s"${c.from}->${c.to}: connector rider recomputation mismatch"
      if !auditClose(wm.max30Pct,c.wall.max30Pct) || !auditClose(wm.max100Pct,c.wall.max100Pct) || !auditClose(wm.above180Seconds,c.wall.above180Seconds) then
        f+=s"${c.from}->${c.to}: connector wall metrics recomputation mismatch"
      if wm.max30Pct>=27 then f+=f"${c.from}->${c.to}: max30 ${wm.max30Pct}%.2f%%"
      if wm.max100Pct>=20 then f+=f"${c.from}->${c.to}: max100 ${wm.max100Pct}%.2f%%"
      if wm.above180Seconds>=90 then f+=f"${c.from}->${c.to}: >180W streak ${wm.above180Seconds}%.1fs"
      val (floor,apps)=applyEvidence(s"${c.from} -> ${c.to}",c.geometry,wm,evidence)
      val physical=wm.physicalSeverity
      val eff=math.max(physical,floor)
      recomputedWalls+=eff
      if !auditClose(physical,c.physicalWall) then f+=s"${c.from}->${c.to}: physical wall severity recomputation mismatch"
      if !auditClose(floor,c.evidenceFloor) then f+=s"${c.from}->${c.to}: evidence floor recomputation mismatch"
      if apps!=c.evidence then f+=s"${c.from}->${c.to}: evidence provenance recomputation mismatch"
      if !auditClose(eff,c.effectiveWall) then f+=s"${c.from}->${c.to}: effective wall recomputation mismatch"
    }
    if recomputedWalls.nonEmpty then
      val req=recomputedWalls.max
      if !auditClose(req,route.requiredWall) then
        f+=f"requiredWall mismatch: stored=${route.requiredWall}%.9f recomputed=$req%.9f"
      if req>ceiling then f+=f"requiredWall $req%.6f > class ceiling $ceiling%.6f"
    finalGeom.sliding(2).foreach {
      case Vector(a,b) =>
        val d=haversine(a,b)
        finalGapLevel(d) match
          case 2 => f+=f"point gap $d%.1fm >=${FinalGapFailM}%.0fm"
          case 1 => w+=f"point gap $d%.1fm"
          case _ => ()
      case _ => ()
    }
    AuditResult(f.toVector.distinct,w.toVector.distinct)


  def xmlEscape(s:String):String = s.flatMap {case '&'=>"&amp;";case '<'=>"&lt;";case '>'=>"&gt;";case '"'=>"&quot;";case '\''=>"&apos;";case c=>c.toString}

  def writeGpx(path:Path,name:String,points:Vector[Point]):Result[Unit] =
    boundary(s"write GPX $path") {
      val writer=Files.newBufferedWriter(path,StandardCharsets.UTF_8)
      try
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        writer.write(
          s"<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\" creator=\"$BuildId\">\n<trk><name>${xmlEscape(name)}</name><trkseg>\n"
        )
        points.foreach { p =>
          writer.write(
            f"<trkpt lat=\"${p.lat}%.7f\" lon=\"${p.lon}%.7f\"><ele>${p.ele}%.6f</ele></trkpt>\n"
          )
        }
        writer.write("</trkseg></trk></gpx>\n")
      finally writer.close()
    }


  def fmtSec(x:Double):String =
    val s=math.round(x).toLong; f"${s/3600}%d:${(s%3600)/60}%02d:${s%60}%02d"
  def fmtShort(x:Double):String =
    val s=math.round(x).toLong; f"${s/60}%d:${s%60}%02d"

  def routeLine(label:String,r:RiderTerminal,ceiling:Double,trails:Vector[Trail],audit:AuditResult):String =
    val order=r.order.map(i=>trails(i).name).mkString(" -> ")
    f"$label | ${r.mode} | wall=${r.requiredWall}%.6f <= $ceiling%.6f | order=$order | transfer=${fmtSec(r.transfer)} | candHard=${fmtShort(r.rider.candHard)} | low=${fmtShort(r.rider.streak120.localMax)} | high=${fmtShort(r.rider.streak140.localMax)} | spike=${r.rider.spike}%.2f | road=${fmtShort(r.road)} | maxAscent=${r.climb.maxAscent}%.1f | upwardViolation=${r.climb.upward}%.1f | roughness=${r.climb.roughness}%.1f | audit=${audit.status}"

  def routeDistanceKm(points:Vector[Point]):Double =
    points.sliding(2).map {
      case Vector(a,b) => haversine(a,b)
      case _ => 0.0
    }.sum / 1000.0

  def routeTransferAscentM(r:RiderTerminal):Double =
    r.connectors.map(_.ascentM).sum

  def routeMaxConnector100Pct(r:RiderTerminal):Double =
    r.connectors.map(_.wall.max100Pct).foldLeft(0.0)(math.max)

  def endpointLabel(mode:Mode):String =
    mode match
      case Mode.LOOP => StartName
      case Mode.P2P => P2PFinishName

  def humanWatchLines(r:RiderTerminal,audit:AuditResult,limit:Int=4):Vector[String] =
    val road =
      r.connectors
        .filter(_.roadStressSeconds > 0.5)
        .sortBy(c => (-c.roadStressSeconds, c.from, c.to))
        .map(c => s"${c.from} -> ${c.to}: road-stress exposure ${fmtShort(c.roadStressSeconds)}")

    val steep =
      r.connectors
        .filter(_.wall.max100Pct > 0.0)
        .sortBy(c => (-c.wall.max100Pct, c.from, c.to))
        .map(c => f"${c.from} -> ${c.to}: max sustained 100 m uphill grade ${c.wall.max100Pct}%.1f%%")

    // Audit warnings are already independent final-output checks.  Put them
    // after road/grade items so day.txt remains useful to a rider, while the
    // complete audit remains in day.debug.txt.
    val auditItems = audit.warnings.sorted
    val all = (road ++ steep ++ auditItems).distinct
    val visible = all.take(limit)
    if all.size > limit then visible :+ s"... ${all.size-limit} more watch item(s); see day.debug.txt"
    else visible

  def humanReport(
      classes:Vector[Double],
      assignment:Assignment,
      finals:Vector[RiderTerminal],
      geometries:Vector[Vector[Point]],
      trails:Vector[Trail],
      audits:Vector[AuditResult]
  ):String =
    val fileNames=OutputGpxFiles
    val b=new StringBuilder
    b.append("MTB DAY PLAN\n============\n")
    b.append(s"Planner build: $BuildId\n\n")
    b.append(s"Mandatory technical GPXs: ${trails.size}.\n")
    b.append(
      "Selected endpoint roles: " +
        finals.indices.map { i =>
          s"DAY-C${i+1} $StartName -> ${endpointLabel(assignment.modes(i))}"
        }.mkString("; ") +
        ".\n"
    )
    val pauseSeconds=trails.size * 2.0 * HumanReportTrailPauseMin * 60.0
    b.append(
      f"Planning-time convention: modeled moving time + ${HumanReportTrailPauseMin}%.0f min before and after each mandatory trail " +
        s"(report only; +${fmtShort(pauseSeconds)} for ${trails.size} trails).\n\n"
    )

    finals.indices.foreach { i =>
      val r=finals(i)
      val audit=audits(i)
      val moving=r.rider.duration
      val planned=moving+pauseSeconds
      val endpoint=endpointLabel(r.mode)
      val order=(Vector(StartName) ++ r.order.map(j=>trails(j).name) ++ Vector(endpoint)).mkString(" -> ")
      b.append(s"DAY-C${i+1} — ${fileNames(i)}\n")
      b.append(
        f"  ${routeDistanceKm(geometries(i))}%.2f km | moving ${fmtSec(moving)} | planned ${fmtSec(planned)} | " +
          f"transfer ${fmtSec(r.transfer)} | transfer +${routeTransferAscentM(r)}%.0f m\n"
      )
      b.append(
        f"  candHard ${fmtShort(r.rider.candHard)} | road ${fmtShort(r.road)} | wall ${r.requiredWall}%.6f <= ${classes(i)}%.6f | " +
          f"max100 ${routeMaxConnector100Pct(r)}%.1f%% | audit ${audit.status}\n"
      )
      b.append(s"  $StartName -> $endpoint\n")
      b.append(s"  $order\n")
      b.append(
        f"  metrics: low=${fmtShort(r.rider.streak120.localMax)} high=${fmtShort(r.rider.streak140.localMax)} " +
          f"spike=${r.rider.spike}%.2f maxAscent=${r.climb.maxAscent}%.1f " +
          f"upwardViolation=${r.climb.upward}%.1f roughness=${r.climb.roughness}%.1f\n"
      )
      humanWatchLines(r,audit).foreach(w => b.append(s"  WATCH: $w\n"))
      b.append("\n")
    }

    if audits.head.status=="WARN" then
      b.append("C1 audit is WARN; review WATCH lines above and day.debug.txt before riding.\n\n")
    else if audits.head.status=="FAIL" then
      b.append("C1 audit is FAIL — do not ride until debugged.\n\n")
    else
      b.append("C1 audit: PASS.\n\n")

    b.append(
      "FILES\n-----\n" +
        "C1 day variant: day.gpx\n" +
        "C2 day variant: day.wall-c2.gpx\n" +
        "C3 day variant: day.wall-c3.gpx\n" +
        "Diagnostics: day.debug.txt\n"
    )
    b.result()

  val ProductionFiles:Set[String] =
    Set("day.gpx","day.wall-c2.gpx","day.wall-c3.gpx","day.txt","day.debug.txt")

  @volatile var LiveDebugPath:Path=Paths.get("day.debug.txt")
  @volatile var LiveDebugStartNanos:Long=System.nanoTime()
  @volatile var LiveDebugLastNanos:Long=LiveDebugStartNanos

  def prepareOutput(dir:Path):Result[Unit] =
    boundary(s"prepare output directory $dir") {
      Files.createDirectories(dir)
      val liveDebug=LiveDebugPath.toAbsolutePath.normalize
      ProductionFiles.foreach { name =>
        val target=dir.resolve(name)
        val isLiveDebug=
          name == "day.debug.txt" &&
            target.toAbsolutePath.normalize == liveDebug
        if !isLiveDebug then Files.deleteIfExists(target)
      }
      ()
    }

  def verifyProductionFiles(dir:Path):Result[Unit] =
    val missing=ProductionFiles.filterNot(name => Files.isRegularFile(dir.resolve(name)))
    if missing.isEmpty then Right(())
    else Left(s"missing production outputs: ${missing.toVector.sorted.mkString(", ")}")


  case class RunResult(classes:Vector[Double],assignment:Assignment,finals:Vector[RiderTerminal],audits:Vector[AuditResult],demanding:Vector[String],diag:Diagnostics,valhallaStatus:String,breaks:Vector[Breakpoint],allAssignments:Vector[Assignment],baselines:Vector[RiderTerminal])

  def runRiderClasses(
      classes:Vector[Double],
      assignment:Assignment,
      loopFront:Vector[RawTerminal],
      p2pFront:Vector[RawTerminal],
      trails:Vector[Trail],
      graph:Map[(String,String),Vector[Connector]],
      diag:Diagnostics
  ):Result[(Vector[RiderTerminal],Vector[RiderTerminal])] =
    def step(
        classIndex:Int,
        finals:Vector[RiderTerminal],
        baselines:Vector[RiderTerminal]
    ):Result[(Vector[RiderTerminal],Vector[RiderTerminal])] =
      if classIndex >= classes.size then Right((finals,baselines))
      else
        val mode=assignment.modes(classIndex)
        val rawFront=if mode == Mode.LOOP then loopFront else p2pFront
        fastestRaw(rawFront,classes(classIndex)) match
          case None => Left(s"no RAW baseline for C${classIndex+1} $mode")
          case Some(rawBase) =>
            val baseline=evaluateRaw(rawBase,trails)
            val started=System.nanoTime()
            val debugLabel=s"C${classIndex+1}-$mode"
            appendLiveDebug(
              f"phase=rider C${classIndex+1} START mode=$mode ceiling=${classes(classIndex)}%.9f baselineCandHard=${baseline.rider.candHard}%.3f baselineLow=${baseline.rider.streak120.localMax}%.3f baselineHigh=${baseline.rider.streak140.localMax}%.3f baselineSpike=${baseline.rider.spike}%.3f baselineRoad=${baseline.road}%.3f"
            )
            val riderFront=riderDp(
              mode,
              classes(classIndex),
              if classIndex == 0 then None else Some(classes(classIndex-1)),
              baseline,
              debugLabel,
              trails,
              graph
            )
            appendLiveDebug(s"phase=rider C${classIndex+1} DONE selectedUpgradeCandidates=${riderFront.size}")
            diag.riderFrontierSizes(s"C${classIndex+1}-${mode}")=riderFront.size
            diag.timings+=(s"rider C${classIndex+1}"->((System.nanoTime()-started)/1e9))
            val selected=chooseFinal(riderFront,baseline,classIndex,classes)
            step(classIndex+1,finals :+ selected,baselines :+ baseline)

    step(0,Vector.empty,Vector.empty)

  def runProduction(input:Path,output:Path,url:String):Result[RunResult] =
    val started=System.nanoTime()
    val diag=Diagnostics()

    prepareOutput(output).flatMap { _ =>
      appendLiveDebug("phase=input-load START")

      loadInputs(input).flatMap { case(trails,avoids,reals) =>
        val demanding=trails.filter(_.demanding.demanding).map(_.name)
        appendLiveDebug(
          s"phase=input-load DONE mandatory=${trails.size} avoid=${avoids.size} real=${reals.size} demanding=${demanding.mkString(",")}"
        )

        buildRealRideEvidence(reals,trails).flatMap { evidence =>
          appendLiveDebug(
            s"phase=real-ride-preprocess DONE corridors=${evidence.size} evidenceCandidates=${evidence.map(_.candidates.size).sum} " +
              s"byTransfer=${evidence.map(e=>s"${e.label}:${e.candidates.size}").mkString("[",",","]")}"
          )

          val client=ValhallaClient(url)
          client.status().flatMap { status =>
            appendLiveDebug(s"phase=valhalla-status DONE ${status.replaceAll("\\s+"," ").take(300)}")
            appendLiveDebug("phase=connector-graph START")
            val graphStarted=System.nanoTime()

            buildGraph(client,trails,avoids,evidence,diag).flatMap { graph =>
              diag.timings+=("connector graph"->((System.nanoTime()-graphStarted)/1e9))
              appendLiveDebug("phase=connector-graph DONE " + graphDiagnostic(trails,graph,diag))
              appendLiveDebug("valhalla-cache " + client.cacheStats)
              graphNodeDiagnostics(trails,graph).foreach(appendLiveDebug)

              val loopReachable=hasCompleteOrder(Mode.LOOP,trails,graph)
              val p2pReachable=hasCompleteOrder(Mode.P2P,trails,graph)
              appendLiveDebug(s"graph-reachability LOOP=$loopReachable P2P=$p2pReachable")

              if !loopReachable || !p2pReachable then
                val missing=Vector(
                  if !loopReachable then Some("LOOP") else None,
                  if !p2pReachable then Some("P2P") else None
                ).flatten.mkString(" and ")
                Left(s"connector graph has no complete mandatory order for $missing")
              else
                val rawStarted=System.nanoTime()
                appendLiveDebug("phase=RAW START")
                val loopFront=rawDp(Mode.LOOP,trails,graph)
                val p2pFront=rawDp(Mode.P2P,trails,graph)
                diag.rawFrontierSizes("LOOP")=loopFront.size
                diag.rawFrontierSizes("P2P")=p2pFront.size
                appendLiveDebug(
                  s"phase=RAW DONE loopFrontier=${loopFront.size} p2pFrontier=${p2pFront.size}"
                )
                loopFront.foreach(t => appendLiveDebug(rawTerminalDiagnostic(t,trails)))
                p2pFront.foreach(t => appendLiveDebug(rawTerminalDiagnostic(t,trails)))

                if loopFront.isEmpty || p2pFront.isEmpty then
                  Left(s"RAW frontier empty: LOOP=${loopFront.size}, P2P=${p2pFront.size}")
                else
                  val breakpointsAll=(breakpoints(loopFront)++breakpoints(p2pFront))
                    .sortBy(b => (b.ceiling,b.mode.toString,b.transfer,b.road,b.signature))
                  val usefulSeverities=breakpointsAll.map(_.ceiling).distinct.sorted
                  appendLiveDebug(
                    s"wall-breakpoints usefulCount=${breakpointsAll.size} distinctUsefulLevels=${usefulSeverities.size}"
                  )

                  if usefulSeverities.size < 3 then
                    val table=breakpointsAll.map(
                      b => f"${b.mode} ${b.ceiling}%.9f ${b.transfer}%.3f ${b.road}%.3f ${b.signature}"
                    ).mkString("\n")
                    Left(s"RAW useful wall selector produced only ${usefulSeverities.size} distinct levels; breakpoints:\n$table")
                  else
                    // Canonical production selector: C1 is the first useful
                    // wall level, C3 the last, and C2 the last interior useful
                    // level. This is intentionally the validated old-product
                    // policy, not an invented numeric target.
                    val classes=Vector(
                      usefulSeverities.head,
                      usefulSeverities.drop(1).dropRight(1).last,
                      usefulSeverities.last
                    )
                    appendLiveDebug(
                      s"derived-wall-classes C1=${classes(0)} C2=${classes(1)} C3=${classes(2)}"
                    )
                    chooseAssignment(classes,loopFront,p2pFront) match
                      case None => Left("expected exactly three feasible endpoint assignments")
                      case Some((assignments,assignment)) =>
                        diag.timings+=("RAW+classes+assignment"->((System.nanoTime()-rawStarted)/1e9))
                        appendLiveDebug(s"endpoint-assignment DONE selected=${assignment.modes.mkString("/")}")

                        runRiderClasses(
                          classes,assignment,loopFront,p2pFront,trails,graph,diag
                        ).flatMap { case(finals,baselines) =>
                          val geometries=finals.map(reconstruct(_,trails))
                          val audits=finals.zip(classes).zip(geometries).zipWithIndex.map {
                            case(((route,ceiling),geometry),index) =>
                              audit(route,ceiling,assignment.modes(index),trails,avoids,evidence,geometry)
                          }

                          val hardFailures=audits.zipWithIndex.flatMap {
                            case(result,index) =>
                              result.failures.map(message => s"C${index+1}: $message")
                          }

                          if hardFailures.nonEmpty then
                            Left("final audit hard failure:\n"+hardFailures.mkString("\n"))
                          else
                            val summary=humanReport(
                              classes,assignment,finals,geometries,trails,audits
                            )

                            val detailedDebug=
                              renderProductionDebug(
                                status,trails,avoids,reals,evidence,graph,diag,
                                breakpointsAll,classes,assignments,assignment,
                                baselines,finals,audits
                              )

                            val writes=Vector[Result[Unit]](
                              writeGpx(output.resolve(OutputGpxFiles(0)),"DAY-C1",geometries(0)),
                              writeGpx(output.resolve(OutputGpxFiles(1)),"DAY-C2",geometries(1)),
                              writeGpx(output.resolve(OutputGpxFiles(2)),"DAY-C3",geometries(2)),
                              boundary("write day.txt") {
                                Files.writeString(output.resolve(OutputSummaryFile),summary,StandardCharsets.UTF_8)
                                ()
                              },
                              boundary("write day.debug.txt") {
                                diag.timings+=("total"->((System.nanoTime()-started)/1e9))
                                val debugPath=output.resolve(OutputDebugFile)
                                val livePrefix =
                                  if Files.isRegularFile(debugPath) then Files.readString(debugPath,StandardCharsets.UTF_8)
                                  else ""
                                val separator =
                                  if livePrefix.isEmpty then ""
                                  else "\n===== FINAL STRUCTURED DEBUG =====\n"
                                Files.writeString(
                                  debugPath,
                                  livePrefix + separator + detailedDebug + renderTimings(diag),
                                  StandardCharsets.UTF_8
                                )
                                ()
                              }
                            )

                            sequence(writes).flatMap { _ =>
                              verifyProductionFiles(output).map { _ =>
                                RunResult(
                                  classes,assignment,finals,audits,demanding,diag,status,
                                  breakpointsAll,assignments,baselines
                                )
                              }
                            }
                        }
            }
          }
        }
      }
    }

  def renderTimings(diag:Diagnostics):String =
    val b=new StringBuilder
    b.append("Timings:\n")
    diag.timings.foreach { case(name,seconds) =>
      b.append(f"  $name: $seconds%.3fs\n")
    }
    b.toString

  def renderProductionDebug(
      status:String,
      trails:Vector[Trail],
      avoids:Vector[Gpx],
      reals:Vector[Gpx],
      evidence:Vector[EvidenceCorridor],
      graph:Map[(String,String),Vector[Connector]],
      diag:Diagnostics,
      breakpointsAll:Vector[Breakpoint],
      classes:Vector[Double],
      assignments:Vector[Assignment],
      assignment:Assignment,
      baselines:Vector[RiderTerminal],
      finals:Vector[RiderTerminal],
      audits:Vector[AuditResult]
  ):String =
    val b=new StringBuilder
    b.append(s"Build id: $BuildId\n")
    b.append(s"Valhalla: $status\n")
    b.append(
      s"Input counts: mandatory=${trails.size}, avoid=${avoids.size}, real=${reals.size}\n"
    )
    b.append("Demanding classification:\n")
    trails.foreach { t =>
      b.append(
        f"  ${t.name}: demanding=${t.demanding.demanding} wholeGrade=${t.demanding.wholeGradePct}%.6f%% sinuosity=${t.demanding.wholeSinuosity}%.6f local60Grade=${t.demanding.local60MaxGradePct}%.6f%% local60Sinu=${t.demanding.local60MaxSinuosity}%.6f pass60=${t.demanding.local60Pass} local100Grade=${t.demanding.local100MaxGradePct}%.6f%% local100Sinu=${t.demanding.local100MaxSinuosity}%.6f pass100=${t.demanding.local100Pass}\n"
      )
    }
    b.append(
      s"Connector candidates generated=${diag.generated}, noRoute=${diag.noRoute}, accepted=${diag.acceptedVariants}, retained=${diag.retained}, safetyReroutes=${diag.safetyReroutes}, safetyBlockedProfiles=${diag.safetyBlockedProfiles}\n"
    )
    if diag.safetyRerouteCorridors.nonEmpty then
      b.append("Route-derived protected-corridor reroutes:\n")
      diag.safetyRerouteCorridors.toVector.sortBy { case(_,count) => -count }.foreach {
        case(name,count) => b.append(s"  $name=$count\n")
      }
    b.append("Hard rejections:\n")
    diag.hardRejects.toVector.sortBy(_._1).foreach {
      case(reason,count) => b.append(s"  $reason=$count\n")
    }
    b.append(s"Connector graph logical transitions=${graph.size}\n")
    b.append(s"Evidence corridors=${evidence.size}; applied variants=${diag.evidenceApplied}\n")
    evidence.foreach { corridor =>
      val c30=corridor.candidates.count(_.windowM==30.0)
      val c100=corridor.candidates.count(_.windowM==100.0)
      b.append(s"  evidence ${corridor.label}: candidates30=$c30 candidates100=$c100\n")
    }
    b.append("RAW frontier sizes: "+diag.rawFrontierSizes.toVector.sortBy(_._1).mkString(", ")+"\n")
    b.append("RAW useful breakpoints:\n")
    breakpointsAll.foreach { bp =>
      b.append(
        f"  ${bp.mode} wall=${bp.ceiling}%.9f transfer=${bp.transfer}%.3f road=${bp.road}%.3f\n"
      )
    }
    b.append(
      "Derived classes: "+
        classes.zipWithIndex.map { case(c,i) => f"C${i+1}=$c%.9f" }.mkString(", ")+
        "\n"
    )
    b.append("Endpoint assignments:\n")
    assignments.foreach { a =>
      b.append(
        s"  modes=${a.modes.mkString("/")} totalTransfer=${a.totalTransfer} totalRoad=${a.totalRoad} p2pIndex=${a.p2pIndex}\n"
      )
    }
    b.append(s"Selected assignment: ${assignment.modes.mkString("/")}\n")
    classes.indices.foreach { i =>
      b.append(
        "RAW baseline "+
          routeLine(
            s"C${i+1}",baselines(i),classes(i),trails,
            AuditResult(Vector.empty,Vector.empty)
          )+
          "\n"
      )
      b.append(
        s"Rider selector outputs C${i+1}: ${diag.riderFrontierSizes.getOrElse(s"C${i+1}-${assignment.modes(i)}",0)}\n"
      )
      b.append(
        "Selected final "+
          routeLine(s"C${i+1}",finals(i),classes(i),trails,audits(i))+
          "\n"
      )
      b.append(s"  Connector diagnostics C${i+1}:\n")
      finals(i).connectors.foreach { connector =>
        val distanceM=connector.geometry.sliding(2).map {
          case Vector(a,b) => haversine(a,b)
          case _ => 0.0
        }.sum
        b.append(
          f"    ${connector.from}->${connector.to} profile=${connector.profile.id} distance=${distanceM}%.1fm transfer=${connectorTransferSeconds(connector)}%.3fs road=${connector.roadStressSeconds}%.3fs max30=${connector.wall.max30Pct}%.3f%% max100=${connector.wall.max100Pct}%.3f%% p180=${connector.wall.above180Seconds}%.3fs physical=${connector.physicalWall}%.9f evidence=${connector.evidenceFloor}%.9f effective=${connector.effectiveWall}%.9f ascent=${connector.ascentM}%.1fm crr=${connector.crr}%.5f\n"
        )
      }
      finals(i).connectors.zipWithIndex.foreach { case(connector,index) =>
        connector.evidence.foreach { app =>
          b.append(f"  EVIDENCE connector=$index ${connector.from}->${connector.to} corridor=${app.corridor} severity=${app.severity}%.6f details=${app.details.mkString(";")}\n")
        }
      }
      audits(i).failures.foreach(failure => b.append(s"  FAIL $failure\n"))
      audits(i).warnings.foreach(warning => b.append(s"  WARN $warning\n"))
    }
    b.toString


  case class TestState(
      var passed:Int=0,
      var failed:Int=0,
      failures:mutable.ArrayBuffer[String]=mutable.ArrayBuffer.empty,
      lines:mutable.ArrayBuffer[String]=mutable.ArrayBuffer.empty
  ):
    def test(name:String)(body: => Unit):Unit =
      Try(body) match
        case Success(_) =>
          passed += 1
          val line=s"PASS $name"
          lines += line
          println(line)
        case Failure(e) =>
          failed += 1
          val detail=s"$name: ${Option(e.getMessage).getOrElse(e.getClass.getSimpleName)}"
          failures += detail
          val line=s"FAIL $detail"
          lines += line
          println(line)
  def assertT(cond:Boolean,msg:String="assertion failed"):Unit=if !cond then throw new AssertionError(msg)
  def near(a:Double,b:Double,eps:Double=1e-6)=assertT(math.abs(a-b)<=eps,s"$a != $b")

  def syntheticLine(length:Double,startEle:Double,endEle:Double,wiggle:Boolean=false):Vector[Point] =
    val lat=53.0; val lon=10.0; val steps=math.max(2,(length/10).toInt+1)
    (0 until steps).map { i =>
      val t=i.toDouble/(steps-1); val dy=length*t; val dx=if wiggle then 8*math.sin(t*math.Pi*6) else 0.0
      Point(lat+dy/EarthR*180/math.Pi,lon+dx/(EarthR*math.cos(math.toRadians(lat)))*180/math.Pi,startEle+t*(endEle-startEle))
    }.toVector

  def syntheticLocalDemand(windowM:Double,dropM:Double):Vector[Point] =
    val lat0=53.0; val lon0=10.0; val cos0=math.cos(math.toRadians(lat0))
    def point(north:Double,east:Double,ele:Double)=Point(lat0+north/EarthR*180/math.Pi,lon0+east/(EarthR*cos0)*180/math.Pi,ele)
    val out=mutable.ArrayBuffer.empty[Point]
    var north=0.0; var east=0.0; var ele=100.0
    out+=point(north,east,ele)
    for _ <- 0 until 20 do {north+=10;out+=point(north,east,ele)}
    val steps=math.round(windowM/10.0).toInt
    for i <- 0 until steps do
      north+=6.0; east += (if i%2==0 then 8.0 else -8.0); ele=100.0-dropM*(i+1).toDouble/steps; out+=point(north,east,ele)
    for _ <- 0 until 20 do {north+=10;out+=point(north,east,ele)}
    out.toVector

  def runSelfTests(input:Option[Path]):TestState =
    val ts=TestState()

    // 1. CLI execution contract.
    ts.test("CLI tests run by default; --no-test skips; --self-test is removed") {
      parseCli(Array.empty) match
        case Right(c) => assertT(c.runTests,"tests must run by default")
        case Left(problem) => assertT(false,problem)
      parseCli(Array("--no-test")) match
        case Right(c) => assertT(!c.runTests,"--no-test must skip tests")
        case Left(problem) => assertT(false,problem)
      assertT(parseCli(Array("--self-test")).isLeft,"removed --self-test unexpectedly accepted")
    }

    // 2. Canonical input/data identity contract.
    ts.test("canonical input counts, NFC identities and demanding set") {
      input match
        case None => assertT(false,"default tests require --input for canonical input regression")
        case Some(in) =>
          loadInputs(in) match
            case Left(problem) => assertT(false,problem)
            case Right((tr,av,real)) =>
              assertT(tr.size==10,s"mandatory=${tr.size}")
              assertT(av.size==10,s"avoid=${av.size}")
              assertT(real.size==4,s"real=${real.size}")
              val ids=tr.map(_.name)
              assertT(ids.distinct.size==ids.size,s"duplicate mandatory identities: $ids")
              assertT(ids.forall(x=>nfc(x)==x),"mandatory identity is not NFC canonical")
              assertT(nfc("Feuerlo\u0308scher")=="Feuerlöscher")
              val got=tr.filter(_.demanding.demanding).map(_.name).toSet
              val exp=Set("Feuerlöscher","LittleWhistlerB")
              assertT(got==exp,s"got ${got.toVector.sorted.mkString(",")}, expected ${exp.toVector.sorted.mkString(",")}")
    }

    // 3. Demanding whole-trail semantics and ridden-length normalization.
    ts.test("demanding whole-trail classification uses ridden length") {
      val demanding=syntheticLine(100,100,70,wiggle=true)
      val dd=demandingMeasurements(demanding)
      assertT(dd.wholeGradePct>=10.0,s"wholeGrade=${dd.wholeGradePct}")
      assertT(dd.wholeSinuosity>=1.10,s"wholeSinuosity=${dd.wholeSinuosity}")
      assertT(dd.demanding)

      val p=syntheticLine(100,100,89,wiggle=true)
      val d=demandingMeasurements(p)
      val expected=100.0*11.0/cumulative(p).last
      near(d.wholeGradePct,expected,1e-6)
      assertT(d.wholeGradePct<11.0,s"grade unexpectedly chord-normalized: ${d.wholeGradePct}")
    }

    // 4. Local demanding-window semantics.
    ts.test("demanding local 60m and 100m windows are independent") {
      val d60=demandingMeasurements(syntheticLocalDemand(60,11.4))
      assertT(d60.local60Pass)
      assertT(!d60.local100Pass)
      assertT(!(d60.wholeGradePct>=10 && d60.wholeSinuosity>=1.10))

      val d100=demandingMeasurements(syntheticLocalDemand(100,16.0))
      assertT(d100.local100Pass)
      assertT(!d100.local60Pass)
      assertT(!(d100.wholeGradePct>=10 && d100.wholeSinuosity>=1.10))
    }

    // 5. Road policy: finite primary is scored, true forbidden classes stay hard.
    ts.test("road policy separates hard invalid roads from scored primary exposure") {
      val geom=syntheticLine(120,100,100)
      val primary=EdgeAttr("e",0,geom.size-1,120.0,100.0,"primary","road","paved","no",false,Double.NaN)
      val modeled=modeledRunSeconds(geom,EdgeRun(Vector(primary),0,geom.size-1))
      assertT(modeled>primary.seconds*2.0,s"modeled=$modeled edgeSpeed=${primary.seconds}")
      assertT(safetyReasons(Vector(primary),geom).isEmpty,s"finite primary unexpectedly hard-rejected: ${safetyReasons(Vector(primary),geom)}")
      assertT(roadStress(Vector(primary),geom)>0.0,"finite primary must remain scored")

      val motorway=primary.copy(id="m",roadClass="motorway")
      val trunk=primary.copy(id="t",roadClass="trunk")
      assertT(safetyReasons(Vector(motorway),geom).contains("motorway"))
      assertT(safetyReasons(Vector(trunk),geom).contains("trunk"))
      assertT(!protectedCycle("none"))
      assertT(!protectedCycle("no"))
      assertT(!protectedCycle("shared"))
      assertT(protectedCycle("dedicated"))
    }

    // 6. Transfer physics contract.
    ts.test("transfer physics uses 30m grade chunks and downhill coasting") {
      val origin=Point(53.0,10.0,100.0)
      def east(m:Double,ele:Double)=Point(
        origin.lat,
        origin.lon+math.toDegrees(m/(EarthR*math.cos(math.toRadians(origin.lat)))),
        ele
      )
      val spike=Vector(east(0,100),east(10,106),east(20,100),east(30,100))
      near(physics(spike,0.010).streak180.localMax,0.0,1e-9)

      val ride=segmentRide(-0.10,0.010,None)
      assertT(ride.coasting,s"ride=$ride")
      near(ride.riderPowerW,0.0,1e-12)
    }

    // 7. All independent hard wall boundaries.
    ts.test("hard wall thresholds are 27pct/30m, 20pct/100m and 180W/90s") {
      assertT(WallMetrics(27.0,0.0,0.0).hardInvalid)
      assertT(WallMetrics(0.0,20.0,0.0).hardInvalid)
      assertT(WallMetrics(0.0,0.0,90.0).hardInvalid)
      assertT(!WallMetrics(26.999,19.999,89.999).hardInvalid)
      val comfort=RiderMetrics(90,90,90,90,Streak.constant(90,true),Streak.constant(90,true),Streak.constant(90,true),1)
      assertT(comfort.candHard>0)
    }

    // 8. Streaks must concatenate across connector/mandatory boundaries.
    ts.test("streak concatenation crosses component boundaries") {
      val a=Streak(0,4,4,false,10)
      val b=Streak(5,0,5,false,10)
      near(a.concat(b).localMax,9)
    }

    // 9. Real-ride evidence remains directional.
    ts.test("real-ride wall evidence is directional") {
      val candidate=syntheticLine(100,0,10,wiggle=false)
      val forward=forwardEvidenceLocalGrade(candidate,candidate,100.0)
      val reverse=forwardEvidenceLocalGrade(candidate,candidate.reverse,100.0)
      assertT(forward.nonEmpty,"forward local evidence failed to match")
      assertT(reverse.isEmpty,"reversed local evidence was incorrectly accepted")
    }

    // 10. Correct continuous tube geometry incl. historical empty-intersection regression.
    ts.test("protected corridor continuous tube geometry invariants") {
      val origin = Point(53.0,10.0,0.0)
      def metres(x:Double,y:Double) = Point(
        origin.lat + math.toDegrees(y / EarthR),
        origin.lon + math.toDegrees(x / (EarthR * math.cos(math.toRadians(origin.lat)))),
        0.0
      )
      def closeMeters(a:Double,b:Double,tol:Double=0.05):Unit =
        assertT(math.abs(a-b)<=tol,f"$a%.4f != $b%.4f within $tol%.3f m")

      val corridor=Vector(metres(-20,0),metres(50,0))
      val boundary=Vector(metres(0,12.1),metres(7.5,6),metres(15,6),metres(22.5,12.1))
      val perpendicular=Vector(metres(10,-20),metres(10,20))
      val oblique=Vector(metres(-2,-14),metres(26,14))
      val forward=Vector(metres(0,0),metres(30,0))
      val outside=Vector(metres(0,12.1),metres(30,12.1))
      val inside=Vector(metres(0,11.9),metres(30,11.9))

      assertT(continuousCoTravel(boundary,corridor,12.0)>20.0)
      assertT(continuousCoTravel(perpendicular,corridor,12.0)<1e-6)
      assertT(continuousCoTravel(oblique,corridor,12.0)<1e-6)
      assertT(continuousCoTravel(forward,corridor,12.0)>29.0)
      closeMeters(continuousCoTravel(forward,corridor,12.0),continuousCoTravel(forward.reverse,corridor,12.0))
      assertT(continuousCoTravel(outside,corridor,12.0)<1e-6)
      assertT(continuousCoTravel(inside,corridor,12.0)>29.0)

      val splitForward=(0 to 6).map(i=>metres(i*5.0,0)).toVector
      val splitCorridor=Vector(metres(-20,0),metres(0,0),metres(15,0),metres(50,0))
      closeMeters(continuousCoTravel(forward,corridor,12.0),continuousCoTravel(splitForward,corridor,12.0))
      closeMeters(continuousCoTravel(forward,corridor,12.0),continuousCoTravel(forward,splitCorridor,12.0))

      val blocker=coTravelBlockPoint(boundary,corridor,12.0,boundary.head,boundary.last)
      assertT(blocker.nonEmpty,"hard co-travel did not produce a blocker")
      assertT(math.min(haversine(blocker.get,boundary.head),haversine(blocker.get,boundary.last))>5.0)

      val oldBugRoute=Vector(
        metres(0,0),metres(10,-20),metres(-20,-50),metres(-15,-50),metres(-5,-70)
      )
      val oldBugCorridor=Vector(
        metres(0,0),metres(-5,5),metres(15,0),metres(5,30),metres(25,60)
      )
      val corrected=continuousCoTravel(oldBugRoute,oldBugCorridor,12.0)
      assertT(corrected<1e-6,f"corrected matcher false-positive=$corrected%.3fm")
    }

    // 11. Safety result must not depend on raw Valhalla vertex segmentation.
    ts.test("corridor safety sampling canonicalizes raw segmentation") {
      val origin=Point(53.0,10.0,0.0)
      def metres(x:Double,y:Double)=Point(
        origin.lat+math.toDegrees(y/EarthR),
        origin.lon+math.toDegrees(x/(EarthR*math.cos(math.toRadians(origin.lat)))),
        0.0
      )
      val corridor=Vector(metres(-20,0),metres(80,0))
      val raw=Vector(metres(0,8),metres(60,8))
      val alreadyDense=(0 to 6).map(i=>metres(i*10.0,8)).toVector
      val a=continuousCoTravel(corridorSafetyGeometry(raw),corridor,12.0)
      val b=continuousCoTravel(corridorSafetyGeometry(alreadyDense),corridor,12.0)
      near(a,b,0.05)
      assertT(a>59.0,s"canonical safety sampling lost parallel co-travel: $a")
    }

    // 12. Exact graph semantics must not collapse nearby-but-different elevation.
    ts.test("semantic connector duplicate is bit exact") {
      def c(id:String,ele:Double)=Connector(
        id,"a","b",Profiles.head,
        Vector(Point(0,0,0),Point(0,0.001,ele)),
        Vector(Point(0,0,0),Point(0,0.001,ele)),
        10,Vector.empty,1,1,0.01,RiderMetrics.Empty,WallMetrics(0,0,0),
        .2,0,.2,Vector.empty,Vector.empty,Vector.empty
      )
      assertT(semanticKey(c("a",10.0000))!=semanticKey(c("b",10.0004)),
        "nearby elevation was incorrectly rounded into semantic equality")
    }

    // 13. RAW DP must enumerate a complete exact-once mandatory order.
    ts.test("RAW DP visits every mandatory exactly once") {
      val dm=DemandingMeasurements(0,1,0,1,false,0,1,false)
      def tr(name:String,x:Double)=Trail(
        name,Vector(Point(53.0,10.0+x,100),Point(53.0,10.00001+x,100)),
        dm,RiderMetrics.Empty
      )
      val trails=Vector(tr("A",0.0),tr("B",0.001),tr("C",0.002))
      def c(id:String,from:String,to:String,sec:Double)=
        val ps=Vector(Point(53,10,100),Point(53,10.00001,100))
        val rm=RiderMetrics(sec,0,0,0,Streak.Empty,Streak.Empty,Streak.Empty,0)
        Connector(id,from,to,Profiles.head,ps,ps,sec,Vector.empty,sec,0,0.01,
          rm,WallMetrics(0,0,0),.2,0,.2,Vector.empty,Vector.empty,Vector.empty)

      val g=Map[(String,String),Vector[Connector]](
        ("START","A")->Vector(c("sa","START","A",1)),
        ("START","B")->Vector(c("sb","START","B",10)),
        ("A","B")->Vector(c("ab","A","B",1)),
        ("A","C")->Vector(c("ac","A","C",10)),
        ("B","A")->Vector(c("ba","B","A",10)),
        ("B","C")->Vector(c("bc","B","C",1)),
        ("C","A")->Vector(c("ca","C","A",10)),
        ("C","B")->Vector(c("cb","C","B",10)),
        ("C","FINISH_LOOP")->Vector(c("cf","C","FINISH_LOOP",1)),
        ("A","FINISH_LOOP")->Vector(c("af","A","FINISH_LOOP",10)),
        ("B","FINISH_LOOP")->Vector(c("bf","B","FINISH_LOOP",10))
      )
      val front=rawDp(Mode.LOOP,trails,g)
      assertT(front.nonEmpty,"RAW DP returned no complete route")
      val best=front.minBy(_.transfer)
      assertT(best.order==Vector(0,1,2),s"best order=${best.order}")
      assertT(best.order.distinct.size==3 && best.order.toSet==Set(0,1,2))
    }

    // 14. Reconstruction: canonical GPXs independently mandatory exactly once, supplied direction.
    ts.test("reconstruction preserves every mandatory exactly once in supplied direction") {
      val dm=DemandingMeasurements(0,1,0,1,false,0,1,false)
      val a=Trail("A",Vector(Point(0,0.001,42),Point(0,0.002,41)),dm,RiderMetrics.Empty)
      val b=Trail("B",Vector(Point(0,0.004,52),Point(0,0.005,51)),dm,RiderMetrics.Empty)
      val trails=Vector(a,b)
      def conn(id:String,from:String,to:String,ps:Vector[Point])=Connector(
        id,from,to,Profiles.head,ps,ps,1,Vector.empty,0,0,0.01,
        RiderMetrics.Empty,WallMetrics(0,0,0),0,0,0,Vector.empty,Vector.empty,Vector.empty
      )
      val enterB=conn("e","START","B",Vector(Point(0,0,0),Point(0,0.004,999)))
      val bToA=conn("ba","B","A",Vector(Point(0,0.005,888),Point(0,0.003,70),Point(0,0.001,777)))
      val finishA=conn("f","A","FINISH_LOOP",Vector(Point(0,0.002,666),Point(0,0.006,0)))
      val route=RiderTerminal(
        Mode.LOOP,0,0,0,RiderMetrics.Empty,ClimbState.Empty,0,0,
        Vector(1,0),Vector(enterB,bToA,finishA),"r"
      )
      val out=reconstruct(route,trails)
      trails.foreach { t =>
        assertT(countSubsequence(out,t.points)==1,s"${t.name} forward count=${countSubsequence(out,t.points)}")
        assertT(countSubsequence(out,t.points.reverse)==0,s"${t.name} reversed mandatory sequence present")
        t.points.foreach(p=>assertT(out.count(q=>exactPoint(q,p))==1,s"${t.name} canonical point/elevation not unique: $p"))
      }
      assertT(out.indexOf(b.points.head)<out.indexOf(a.points.head),"supplied solver order was not preserved")
    }

    // 15. RAW class derivation contract.
    ts.test("wall breakpoint sweep preserves useful wall/order changes") {
      val f=Vector(
        RawTerminal(Mode.LOOP,.2,500,10,Vector(0),Vector(),"a"),
        RawTerminal(Mode.LOOP,.3,300,10,Vector(0),Vector(),"b"),
        RawTerminal(Mode.LOOP,.4,290,5,Vector(1),Vector(),"c")
      )
      assertT(breakpoints(f).map(_.ceiling)==Vector(.2,.3,.4))
    }

    // 16. Product requires exactly two LOOP classes and C3 P2P for this fixture.
    ts.test("endpoint assignment chooses exactly one P2P class") {
      def r(m:Mode,w:Double,t:Double,s:String)=RawTerminal(m,w,t,0,Vector(),Vector(),s)
      val c=Vector(.2,.3,.4)
      val l=Vector(r(Mode.LOOP,.2,10,"l1"),r(Mode.LOOP,.3,9,"l2"),r(Mode.LOOP,.4,8,"l3"))
      val p=Vector(r(Mode.P2P,.2,20,"p1"),r(Mode.P2P,.3,15,"p2"),r(Mode.P2P,.4,5,"p3"))
      chooseAssignment(c,l,p) match
        case Some((all,a)) =>
          assertT(all.size==3)
          assertT(a.modes.count(_==Mode.P2P)==1)
          assertT(a.modes.count(_==Mode.LOOP)==2)
          assertT(a.p2pIndex==2)
        case None => assertT(false,"assignment unexpectedly infeasible")
    }

    // 17. Incremental climb metrics are continuation-state semantics for exact DP.
    ts.test("incremental climb-shape update") {
      var c=ClimbState.Empty
      c=c.add(99,0).add(88,1).add(10,2).add(20,3).add(15,4)
      near(c.maxAscent,20)
      near(c.upward,10)
      near(c.roughness,15)
    }

    // 18. Independent final audit must recompute technical mandatory physics with the downhill cap.
    ts.test("audit rider recomputation preserves technical mandatory policy") {
      val mandatoryPoints=syntheticLine(120,120,90)
      val mandatoryRider=physics(mandatoryPoints,0.010,Some(TrailDownhillMaxKph))
      val mandatory=Trail(
        "T",mandatoryPoints,
        DemandingMeasurements(0,1,0,1,false,0,1,false),
        mandatoryRider
      )
      def conn(id:String,from:String,to:String)=
        val ps=Vector(Point(53.0,10.0,100),Point(53.0,10.00001,100))
        val rm=physics(ps,0.010)
        Connector(
          id,from,to,Profiles.head,ps,ps,1,Vector.empty,rm.duration,0,0.01,
          rm,WallMetrics(0,0,0),0,0,0,Vector.empty,Vector.empty,Vector.empty
        )
      val enter=conn("e","START","T")
      val finish=conn("f","T","FINISH_LOOP")
      val stored=enter.rider.concat(mandatory.rider).concat(finish.rider)
      val route=RiderTerminal(Mode.LOOP,0,0,0,stored,ClimbState.Empty,0,0,Vector(0),Vector(enter,finish),"r")
      assertT(auditSameRider(recomputeRouteRider(route,Vector(mandatory)),stored))
      assertT(!auditSameRider(physics(mandatoryPoints,0.010),mandatoryRider),
        "fixture must distinguish transfer vs technical downhill physics")
    }

    // 19. No fixed search horizon + local marginal-drop post-search selector.
    ts.test("exact no-horizon rider search + local marginal-drop selector") {
      val st=Streak.constant(1.0,false)
      def rt(sig:String,t:Double,ch:Double)=
        RiderTerminal(
          Mode.LOOP,t,0,.2,
          RiderMetrics(t,ch,0,0,st,st,st,0),
          ClimbState.Empty,0,0,Vector(),Vector(),sig
        )
      val baseline=rt("baseline",0,120)
      val p0=rt("p0",10,100)
      val p1=rt("p1",20,80)
      val elbow=rt("elbow",30,60)
      val tail1=rt("tail1",100,59)
      val tail2=rt("tail2",1000,58.5)
      assertT(
        chooseFinal(Vector(p0,p1,elbow,tail1),baseline,0,Vector(.3,.4,.5)).signature=="elbow",
        "local marginal selector must choose the point before the sharp benefit collapse"
      )
      assertT(
        chooseFinal(Vector(p0,p1,elbow,tail1,tail2),baseline,0,Vector(.3,.4,.5)).signature=="elbow",
        "far low-benefit comfort tail must not move this local elbow fixture"
      )

      // FIX50/FIX51 regression: use a compact rounded projection of the real C2
      // frontier. The old global-extrema-normalized knee MUST move when a far
      // comfort endpoint is appended; this proves the fixture actually captures
      // the historical failure. The local marginal selector MUST NOT move because
      // the added point does not change the adjacent segments around its elbow.
      val gFast=rt("g-fast",6279.661,1385.969)
      val gEarly=rt("g-early",6415.052,1244.221)
      val gElbow=rt("g-elbow",6443.442,1179.960)
      val gOldComfort=rt("g-old-comfort",8606.336,637.267)
      val gFarComfort=rt("g-far-comfort",9933.077,571.948)
      val regressionBase=Vector(gFast,gEarly,gElbow,gOldComfort)
      val regressionExtended=regressionBase :+ gFarComfort

      val oldGlobalBase=selectParetoKnee(regressionBase).get.selected.signature
      val oldGlobalExtended=selectParetoKnee(regressionExtended).get.selected.signature
      assertT(
        oldGlobalBase != oldGlobalExtended,
        "fixture must reproduce the old global-extrema knee search-space-extension instability"
      )

      val localBase=selectLocalMarginalDrop(regressionBase).get.selected.signature
      val localExtended=selectLocalMarginalDrop(regressionExtended).get.selected.signature
      assertT(localBase=="g-elbow", "fixture local elbow must be the intended middle point")
      assertT(
        localExtended==localBase,
        "far low-benefit comfort-tail extension must not move an unchanged local elbow"
      )

      val dm=DemandingMeasurements(0,1,0,1,false,0,1,false)
      val trail=Trail("T",Vector(Point(53,10,100),Point(53,10.00001,100)),dm,RiderMetrics.Empty)
      def c(id:String,from:String,to:String,duration:Double,candHard:Double)=
        val ps=Vector(Point(53,10,100),Point(53,10.00001,100))
        val rm=RiderMetrics(
          duration,candHard,0,0,
          Streak.constant(duration,false),Streak.constant(duration,false),Streak.constant(duration,false),0
        )
        Connector(
          id,from,to,Profiles.head,ps,ps,duration,Vector.empty,0,0,0.01,
          rm,WallMetrics(0,0,0),.2,0,.2,Vector.empty,Vector.empty,Vector.empty
        )
      val noHorizonBaseline=rt("no-horizon-baseline",10,10)
      val start=c("slow-comfort","START","T",1010,5)
      val finish=c("finish","T","FINISH_LOOP",0,0)
      val front=riderDp(
        Mode.LOOP,.3,None,noHorizonBaseline,"selftest-no-horizon",
        Vector(trail),Map(("START","T")->Vector(start),("T","FINISH_LOOP")->Vector(finish))
      )
      assertT(front.size==1)
      assertT(front.head.transfer>=1000)
      assertT(front.head.rider.candHard==5)
    }

    // 20. Comfort upgrades may not silently worsen guarded product/safety resources.
    ts.test("rider product selector preserves guardrails while improving candHard") {
      val baseStreak=Streak.constant(10,false)
      val base=RiderTerminal(
        Mode.LOOP,100,10,.3,
        RiderMetrics(100,20,0,0,baseStreak,baseStreak,baseStreak,10),
        ClimbState(0,10,10,10,None,None),
        2,1,Vector(),Vector(),"base"
      )
      def candidate(sig:String,road:Double,spike:Double,maxAscent:Double)=
        RiderTerminal(
          Mode.LOOP,110,road,.3,
          RiderMetrics(110,10,0,0,baseStreak,baseStreak,baseStreak,spike),
          ClimbState(0,maxAscent,10,10,None,None),
          2,1,Vector(),Vector(),sig
        )
      assertT(eligibleUpgrade(candidate("good",10,10,10),base,0,Vector(.3,.4,.5)))
      assertT(!eligibleUpgrade(candidate("road-worse",11,10,10),base,0,Vector(.3,.4,.5)))
      assertT(!eligibleUpgrade(candidate("spike-worse",10,11,10),base,0,Vector(.3,.4,.5)))
      assertT(!eligibleUpgrade(candidate("climb-worse",10,10,11),base,0,Vector(.3,.4,.5)))
    }

    // 21. Human-facing output and exact output-file surface are product contracts.
    ts.test("human report and exact five output filenames remain stable") {
      assertT(OutputFileSet==Vector(
        "day.gpx","day.wall-c2.gpx","day.wall-c3.gpx","day.txt","day.debug.txt"
      ),s"output files=$OutputFileSet")

      val dm=DemandingMeasurements(0,1,0,1,false,0,1,false)
      val trail=Trail("T",Vector(Start,Start),dm,RiderMetrics.Empty)
      def rt(mode:Mode,sig:String)=RiderTerminal(
        mode,0,0,.2,RiderMetrics.Empty,ClimbState.Empty,0,0,Vector(0),Vector(),sig
      )
      val finals=Vector(rt(Mode.LOOP,"c1"),rt(Mode.LOOP,"c2"),rt(Mode.P2P,"c3"))
      val assignment=Assignment(
        Vector(.2,.3,.4),Vector(Mode.LOOP,Mode.LOOP,Mode.P2P),Vector.empty
      )
      val report=humanReport(
        Vector(.2,.3,.4),assignment,finals,
        Vector(Vector(Start),Vector(Start),Vector(Start)),
        Vector(trail),
        Vector.fill(3)(AuditResult(Vector.empty,Vector.empty))
      )
      OutputGpxFiles.foreach(name=>assertT(report.contains(name),s"report missing $name"))
      assertT(report.contains(OutputDebugFile))
      assertT(report.contains("Planning-time convention:"))
      assertT(report.contains("report only"))
      assertT(report.contains(s"DAY-C3 $StartName -> $P2PFinishName"))
    }

    // 22. Independent final reconstructed GPX gap contract.
    ts.test("final GPX gap thresholds are WARN at 100m and FAIL at 250m") {
      assertT(finalGapLevel(99.999)==0)
      assertT(finalGapLevel(100.0)==1)
      assertT(finalGapLevel(249.999)==1)
      assertT(finalGapLevel(250.0)==2)
      assertT(FinalGapWarnM==100.0 && FinalGapFailM==250.0)
    }

    ts
  def debugPathFromArgs(args:Array[String]):Path =
    val idx=args.indexOf("--output")
    if idx>=0 && idx+1<args.length then Paths.get(args(idx+1)).resolve(OutputDebugFile)
    else Paths.get("day.debug.txt")

  def writeAlwaysDebug(path:Path,body:String,append:Boolean=false):Unit =
    val result=Try {
      val parent=path.toAbsolutePath.normalize.getParent
      if parent != null then Files.createDirectories(parent)
      if append then
        Files.writeString(
          path,body,StandardCharsets.UTF_8,
          StandardOpenOption.CREATE,StandardOpenOption.APPEND
        )
      else
        Files.writeString(
          path,body,StandardCharsets.UTF_8,
          StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING
        )
    }
    result.failed.foreach { e =>
      System.err.println(
        s"ERROR: could not write debug ${path.toAbsolutePath}: ${Option(e.getMessage).getOrElse(e.getClass.getSimpleName)}"
      )
    }


  def appendLiveDebug(line:String):Unit = this.synchronized {
    val now=System.nanoTime()
    val elapsed=(now-LiveDebugStartNanos)/1e9
    val delta=(now-LiveDebugLastNanos)/1e9
    LiveDebugLastNanos=now
    val prefix=f"[${Instant.now()} +$elapsed%.3fs Δ$delta%.3fs] "
    writeAlwaysDebug(LiveDebugPath, prefix + line + "\n", append=true)
  }

  def resetLiveDebug(args:Array[String]):Unit = this.synchronized {
    LiveDebugPath=debugPathFromArgs(args)
    LiveDebugStartNanos=System.nanoTime()
    LiveDebugLastNanos=LiveDebugStartNanos
    writeAlwaysDebug(
      LiveDebugPath,
      s"Build id: $BuildId\nArgs: ${args.mkString(" ")}\nExecution status: RUNNING\n"
    )
  }

  def selfTestDebug(ts:TestState,args:Array[String]):String =
    val b = new StringBuilder
    b.append(s"Build id: $BuildId\n")
    b.append("Invocation: self-test\n")
    b.append(s"Args: ${args.mkString(" ")}\n")
    ts.lines.foreach(x => b.append(x).append("\n"))
    b.append(s"SELF-TESTS: ${ts.passed} passed, ${ts.failed} failed\n")
    if ts.failures.nonEmpty then
      b.append("Failures:\n")
      ts.failures.foreach(x => b.append("  ").append(x).append("\n"))
    b.toString

  val Usage: String =
    s"""MTB route planner — $BuildId
      |Usage:
      |  ./trail-plan.scala --input DIR --output DIR --valhalla-url URL
      |  ./trail-plan.scala --input DIR --output DIR --valhalla-url URL --no-test
      |  ./trail-plan.scala --help
      |
      |Tests run by default before production routing. Use --no-test to skip them.
      |""".stripMargin

  def main(args:Array[String]):Unit =
    val debugPath=debugPathFromArgs(args)
    resetLiveDebug(args)

    parseCli(args) match
      case Left(problem) =>
        val line=s"ERROR: $problem"
        System.err.println(line)
        writeAlwaysDebug(debugPath,s"Execution status: FAIL\n$line\n",append=true)
        sys.exit(1)

      case Right(cli) if cli.help =>
        println(Usage)
        writeAlwaysDebug(
          debugPath,
          s"Build id: $BuildId\nInvocation: help\nArgs: ${args.mkString(" ")}\nSTATUS: PASS\n"
        )

      case Right(cli) =>
        (cli.input,cli.output,cli.valhallaUrl) match
          case (Some(input),Some(output),Some(url)) =>
            val testsOpt=
              if cli.runTests then
                val tests=runSelfTests(Some(input))
                val summary=s"SELF-TESTS: ${tests.passed} passed, ${tests.failed} failed"
                tests.lines.foreach(println)
                println(summary)
                appendLiveDebug(summary)
                if tests.failed > 0 then
                  val line="ERROR: default self-tests failed; production routing was not started"
                  System.err.println(line)
                  writeAlwaysDebug(debugPath,selfTestDebug(tests,args)+line+"\n",append=true)
                  sys.exit(2)
                Some(tests)
              else
                appendLiveDebug("SELF-TESTS: skipped by --no-test")
                None

            runProduction(input,output,url) match
              case Left(problem) =>
                val line=s"ERROR: $problem"
                System.err.println(line)
                writeAlwaysDebug(debugPath,s"Execution status: FAIL\n$line\n",append=true)
                sys.exit(1)

              case Right(result) =>
                val testText=testsOpt.map(t=>s" tests=${t.passed}/${t.passed+t.failed}").getOrElse(" tests=SKIPPED")
                val line=
                  s"build=$BuildId demanding=${result.demanding.mkString(",")} classes=${result.classes.mkString(",")} modes=${result.assignment.modes.mkString(",")} audits=${result.audits.map(_.status).mkString(",")}$testText"
                println(line)
                writeAlwaysDebug(debugPath,s"Execution status: PASS\n$line\n",append=true)

          case _ =>
            val missing=Vector(
              if cli.input.isEmpty then Some("--input DIR") else None,
              if cli.output.isEmpty then Some("--output DIR") else None,
              if cli.valhallaUrl.isEmpty then Some("--valhalla-url URL") else None
            ).flatten.mkString(", ")
            val line=s"ERROR: missing required arguments: $missing"
            System.err.println(line)
            writeAlwaysDebug(debugPath,s"Execution status: FAIL\n$line\n",append=true)
            sys.exit(1)
