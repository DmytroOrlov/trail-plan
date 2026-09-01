#!/usr/bin/env -S instant-scala
//> using scala "3.3.7"
//> using dep "com.lihaoyi::ujson:4.4.3"
//> using dep "com.github.alexarchambault::case-app:2.1.0"
//> using packaging.graalvmArgs --no-fallback
// PRODUCT BUILD MTB-CANONICAL-REFERENCE2
// Every supplied technical GPX is independently mandatory exactly once in its
// supplied direction. Canonical mandatory GPX <ele> is used directly.
// Transfers come from Valhalla; explicit trails/avoid may not be transfer
// corridors. Road safety, absolute wall safety and production real-ride wall
// evidence remain hard.
//
// PRODUCT CONTRACT
// ================
// - 12 production Valhalla search profiles.
// - Terrain frontier is exact fastest-transfer at every real wall breakpoint.
// - C1/C2/C3 wall usefulness derives from RAW terrain: first reachable level,
//   then >=3 min raw transfer improvement or a natural raw mandatory-order change.
// - At least one non-demanding warm-up before the first demanding descent is hard.
// - Second warm-up and avoiding demanding->demanding adjacency are soft preferences.
// - Product set contains exactly two LOOP days and one P2P day; wall classes come
//   from the union of RAW-useful terrain breakpoints, while endpoint role is selected
//   independently from all reachable class/endpoint pairings by total RAW transfer.
// - Rider GPX uses the minimum-transfer exact active-wall route that strictly
//   improves candidate 120/140/160 comfort over the migration reference without
//   worsening the guarded rider metrics.
// - No percentage detour budget, family semantics, forced order diversity, beam,
//   top-K route cutoff, DP objective quantization, or random pruning.
//
// EXACTNESS CONTRACT
// ==================
// - Terrain DP is exact on the corrected connector graph it receives.
// - Migration-reference DP is exact inside its legacy +60 envelope.
// - V5 promotion DP is exact inside PromotionSearchSlackCeilingSeconds and uses
//   constrained resource-aware dominance; budget pruning uses an admissible
//   completion lower bound.
// - Mandatory technical GPXs remain exactly once and in supplied direction.
//
// CLEANUP LEDGER
// ==============
// CLEAN7 CANDIDATE-POWER-ONLY:
// - removes the legacy fixed-power comfort system from production/search semantics:
//   weighted fetch effort at 90/150/200/250 W, suffering at 150/200/250 W,
//   legacy 150/180 W streak comfort and legacy 150/50 W spike load;
// - connector generation/semantic dedupe/Pareto dominance now use only V5 candidate
//   comfort (rider-relative 1.50/1.75/2.00x, streak 1.50/1.75x, spike 1.50/0.50x)
//   plus the separate hard 180 W / 90 s safety wall signal;
// - removes old power fields from RideTimeEstimate, Connector, HumanQualityRoute and
//   MultiLabelRoute; reports say candHard instead of legacy hard/suffer;
// - migration +60 remains temporarily, but its comparator/resources now use V5
//   candidate comfort only; it no longer depends on 150/200/250 legacy power metrics;
// - this intentionally changes connector-search ordering/dominance and may change the
//   connector graph. Re-run full class/endpoint/V5/audit regression before promotion.
// CLEAN6-FIX1 REPORT-SYNTAX FIX:
// - removes one dangling string-concatenation '+' left after evaluator deletion;
// - no routing/product behavior changes; report text is otherwise unchanged;
// - production C1/C2/C3, RAW endpoint assignment, migration baselines, safety and Audit.run unchanged.
// CLEAN6:
// - physically removes the manual-order evaluator CLI, output file, fixed-order
//   analysis, bottlenecks, swaps, relocations, transition deltas and greedy search;
// - removes the completed CLEAN5 RAW-baseline replacement audit and its extra exact
//   promotion-frontier computation; Archive 44 showed RAW-fastest is not a sufficient
//   standalone replacement for the current V5 migration baseline;
// - production C1/C2/C3 selection, endpoint-neutral RAW assignment, migration references,
//   safety, final GPX reconstruction and Audit.run semantics are unchanged.
//
// KEEP / PRODUCT:
// - RAW terrain wall breakpoints and RAW-useful global class derivation.
// - Endpoint-neutral assignment over every reachable 2xLOOP + 1xP2P pairing.
// - Candidate comfort cumulative 120/140/160 plus guarded road/climb/streak/spike.
// - Three legacy +60 migration references ONLY as current V5 no-regression baselines.
//
// REMOVE / PROMOTE NEXT:
// - Replace the remaining 3 migration references with an independently validated
//   standalone final preference policy; then delete the last legacy +60 solver.
// - Revalidate/replace PromotionSearchSlackCeilingSeconds if product completeness
//   must extend beyond the currently validated canonical dataset.
// - Revalidate the 12-profile connector cover when canonical data/semantics change.
//
// ALREADY REMOVED:
// - Entire manual-order evaluator CLI/output/runtime.
// - Completed RAW-baseline replacement probe after Archive 44.
// - Endpoint-specific +60 USEFUL/NOISE classification and its replacement audits.
// - Broad comfort/LOW/STOP/RAW-GUARD views, knee/percentage/set diagnostics.
// - Connector-promotion audit runtime after 0/0 semantic/dominance losses.
// - Shadow GPX/report generation after 3/3 acceptance PASS.
// - Legacy +60 influence on wall classes, endpoint assignment and rider GPX choice.

import java.io.FileInputStream
import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.Instant
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.stream.XMLOutputFactory
import org.w3c.dom.{Element, Node}

import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.Try
import caseapp.*

object BuildInfo:
  val id = "MTB-CANONICAL-REFERENCE2-FIX27-CONTRACT-TESTS"

object PowerPolicy:
  // HARD SAFETY ONLY. This is not a rider-comfort preference.
  val SafetyWallPowerW: Int = 180
  val SafetyWallMinStreakSeconds: Double = 90.0

  // PRODUCT RIDER-COMFORT POLICY.
  val CandidateComfortMultipliers: Vector[Double] =
    Vector(1.50, 1.75, 2.00)

  val CandidateStopStreakMultipliers: Vector[Double] =
    Vector(1.50, 1.75)

  val CandidateSpikeBaseMultiplier: Double = 1.50
  val CandidateSpikeScaleMultiplier: Double = 0.50

  def roundedThresholdW(targetW: Double, multiplier: Double): Int =
    math.max(1, math.round(targetW * multiplier).toInt)

  def candidateComfortThresholdsW(targetW: Double): Vector[Int] =
    CandidateComfortMultipliers
      .map(multiplier => roundedThresholdW(targetW, multiplier))
      .distinct
      .sorted

  def candidateStopStreakThresholdsW(targetW: Double): Vector[Int] =
    CandidateStopStreakMultipliers
      .map(multiplier => roundedThresholdW(targetW, multiplier))
      .distinct
      .sorted

  def candidateSpikeBaseW(targetW: Double): Double =
    targetW * CandidateSpikeBaseMultiplier

  def candidateSpikeScaleW(targetW: Double): Double =
    targetW * CandidateSpikeScaleMultiplier

  def trackedThresholdsW(targetW: Double): Vector[Int] =
    (
      candidateComfortThresholdsW(targetW) ++
        candidateStopStreakThresholdsW(targetW) ++
        Vector(SafetyWallPowerW)
    ).distinct.sorted


case class Point(lat: Double, lon: Double, ele: Option[Double] = None)

case class Trail(path: Path, name: String, points: Vector[Point]):
  def start: Point = points.head
  def end: Point = points.last

case class RouteResult(
    from: Point,
    to: Point,
    // Resampled/elevated profile used for rider physics and wall metrics.
    points: Vector[Point],
    // Exact decoded /route shape before resampling. Hard road/downhill
    // trace_attributes edge_walk MUST use this geometry.
    rawValhallaPoints: Vector[Point],
    seconds: Double,
    lengthKm: Double
)

case class SearchProfileKey(speedKph: Double, useHills: Double, useRoads: Double):
  def short: String =
    f"speed=$speedKph%.0f hills=$useHills%.2f roads=$useRoads%.2f"

case class Connector(
    route: RouteResult,
    ascentM: Double,
    descentM: Double,
    lateAscentM: Double,
    physicsSeconds: Double,
    fatiguePenaltySeconds: Double,
    transferQualityPenaltySeconds: Double,
    candidateComfortPenaltySeconds: Double,
    maxRiderPowerW: Double,
    maxGrade30Pct: Double,
    maxGrade100Pct: Double,
    majorRoadSeconds: Double,
    motorwayTrunkSeconds: Double,
    hasMotorwayTrunk: Boolean,
    primaryNoCycleSeconds: Double,
    primarySharedSeconds: Double,
    primaryWithCycleSeconds: Double,
    secondaryNoCycleSeconds: Double,
    secondarySharedSeconds: Double,
    longestLowProtectionPrimarySeconds: Double,
    unpavedSeconds: Double,
    downhillHandlingSeconds: Double,
    longestTechnicalDownhillRunM: Double,
    maxTechnicalDownhillGrade30Pct: Double,
    maxTechnicalDownhillGrade100Pct: Double,
    maxTechnicalPathDownhillGrade30Pct: Double,
    effectiveCrr: Double,
    pathFraction: Double,
    routingSpeedKph: Double,
    routingUseHills: Double,
    routingUseRoads: Double,
    traceEdges: Vector[TraceEdge],
    powerAboveSecondsByThreshold: Map[Int, Double] = Map.empty,
    longestPowerStreakSecondsByThreshold: Map[Int, Double] = Map.empty,
    candidateComfortSpikeLoadSeconds: Double = 0.0,
    searchConnectorVariants: Vector[Connector] = Vector.empty
)

case class ProfileStats(
    lengthM: Double,
    ascentM: Double,
    descentM: Double,
    netElevationM: Option[Double],
    longestClimbDistanceM: Double,
    longestClimbAvgGradePct: Double,
    maxGrade30Pct: Double,
    maxGrade100Pct: Double,
    maxPointGapM: Double
)

case class TraceEdge(
    lengthKm: Double,
    surface: String,
    use: String,
    roadClass: String,
    unpaved: Boolean,
    cycleLane: String,
    beginShapeIndex: Option[Int] = None,
    endShapeIndex: Option[Int] = None
)

case class TraceAttributesDetailed(edges: Vector[TraceEdge], shape: Vector[Point])

case class AuditResult(warnings: Vector[String], failures: Vector[String]):
  def verdict: String =
    if failures.nonEmpty then "FAIL"
    else if warnings.nonEmpty then "WARN"
    else "PASS"

case class RoutingProfile(
    speedKph: Double = 20.0,
    useHills: Double = 0.70,
    useRoads: Double = 0.35,
    bicycleType: String = "Mountain",
    avoidBadSurfaces: Double = 0.50
)

case class Config(
    // The two coordinates are candidate ride endpoints. The planner may start
    // at either and may finish at either: 1->1, 1->2, 2->1, 2->2.
    start: Point = Point(53.465204, 9.962392),
    startName: String = "S-Heimfeld",
    finish: Point = Point(53.472143, 9.876907),
    finishName: String = "S-Neuwiedenthal",
    inputs: Vector[Path] = Vector.empty,
    out: Path = Path.of("day.gpx"),
    report: Option[Path] = None,
    valhalla: String = "http://localhost:8002",
    profile: RoutingProfile = RoutingProfile(),
    arrivalClimbPenaltyMinPer100m: Double = 2.00,
    arrivalWindowM: Double = 500.0,
    physicsSampleM: Double = 10.0,
    riderWeightKg: Double = 65.0,
    bikeWeightKg: Double = 20.0,
    riderPowerW: Double = 80.0,
    downhillMaxKph: Double = 6.0,
    trailPauseMin: Double = 3.0,
    runTests: Boolean = true
)

case class CliOptions(
    start: String = "53.465204,9.962392,S-Heimfeld",
    finish: String = "53.472143,9.876907,S-Neuwiedenthal",
    out: String = "day.gpx",
    report: Option[String] = None,
    valhalla: String = "http://localhost:8002",
    bike: String = "Mountain",
    avoidBadSurfaces: Double = 0.50,
    arrivalClimbPenalty: Double = 2.0,
    riderWeight: Double = 65.0,
    bikeWeight: Double = 20.0,
    power: Double = 80.0,
    trailMaxSpeed: Double = 6.0,
    trailPauseMin: Double = 3.0,
    noTest: Boolean = false
)

object Cli:
  private val ValidBikes = Set("road", "hybrid", "city", "cross", "mountain")

  def parse(args: Seq[String]): Either[String, Option[Config]] =
    CaseApp.parseWithHelp[CliOptions](args) match
      case Left(error) =>
        Left(error.message)

      case Right((parsed, helpAsked, usageAsked, remaining)) =>
        if helpAsked || usageAsked then
          val messages = caseapp.core.help.Help[CliOptions].withHelp
          println(if helpAsked then messages.help else messages.usage)
          Right(None)
        else
          parsed.left.map(_.message).flatMap(options => toConfig(options, remaining).map(Some(_)))

  private def toConfig(options: CliOptions, remaining: Seq[String]): Either[String, Config] =
    for
      start <- endpoint(options.start, "Endpoint 1")
      finish <- endpoint(options.finish, "Endpoint 2")
      _ <- validate(options)
      out <- path(options.out, "--out")
      report <- options.report match
        case Some(raw) => path(raw, "--report").map(Some(_))
        case None      => Right(None)
      explicitInputs <- inputPaths(remaining)
      inputs =
        if explicitInputs.nonEmpty then explicitInputs
        else Inputs.defaultTrailInputs()
      _ <- Either.cond(
        inputs.nonEmpty,
        (),
        "No input trails found. Default is trails/*.gpx; add GPX files there or pass GPX files/directories explicitly."
      )
    yield Config(
      start = start._1,
      startName = start._2,
      finish = finish._1,
      finishName = finish._2,
      inputs = inputs,
      out = out,
      report = report,
      valhalla = options.valhalla.stripSuffix("/"),
      profile = RoutingProfile(bicycleType = options.bike, avoidBadSurfaces = options.avoidBadSurfaces),
      arrivalClimbPenaltyMinPer100m = options.arrivalClimbPenalty,
      riderWeightKg = options.riderWeight,
      bikeWeightKg = options.bikeWeight,
      riderPowerW = options.power,
      downhillMaxKph = options.trailMaxSpeed,
      trailPauseMin = options.trailPauseMin,
      runTests = !options.noTest
    )

  private def endpoint(raw: String, defaultName: String): Either[String, (Point, String)] =
    raw.split(",", 3).map(_.trim) match
      case Array(latRaw, lonRaw) =>
        point(latRaw, lonRaw).map(_ -> defaultName)

      case Array(latRaw, lonRaw, name) =>
        point(latRaw, lonRaw).map(_ -> Option(name).filter(_.nonEmpty).getOrElse(defaultName))

      case _ =>
        Left(s"Expected LAT,LON[,NAME], got: $raw")

  private def point(latRaw: String, lonRaw: String): Either[String, Point] =
    for
      lat <- latRaw.toDoubleOption.toRight(s"Invalid latitude: $latRaw")
      lon <- lonRaw.toDoubleOption.toRight(s"Invalid longitude: $lonRaw")
    yield Point(lat, lon)

  private def validate(options: CliOptions): Either[String, Unit] =
    Vector(
      Option.when(options.avoidBadSurfaces < 0.0 || options.avoidBadSurfaces > 1.0)(
        "--avoid-bad-surfaces must be between 0 and 1"
      ),
      Option.when(options.arrivalClimbPenalty < 0.0)("--arrival-climb-penalty must be >= 0"),
      Option.when(options.riderWeight <= 0.0)("--rider-weight must be > 0"),
      Option.when(options.bikeWeight <= 0.0)("--bike-weight must be > 0"),
      Option.when(options.power <= 0.0)("--power must be > 0"),
      Option.when(options.trailMaxSpeed <= 0.0)("--trail-max-speed must be > 0"),
      Option.when(options.trailPauseMin < 0.0 || options.trailPauseMin > 30.0)(
        "--trail-pause-min must be between 0 and 30"
      ),
      Option.when(!ValidBikes(options.bike.toLowerCase))(
        s"--bike must be one of: ${ValidBikes.toSeq.sorted.mkString(", ")}"
      )
    ).flatten.headOption.toLeft(())

  private def path(raw: String, option: String): Either[String, Path] =
    Try(Path.of(raw)).toEither.left.map(_ => s"$option contains an invalid path: $raw")

  private def inputPaths(raw: Seq[String]): Either[String, Vector[Path]] =
    raw.foldLeft[Either[String, Vector[Path]]](Right(Vector.empty)) { (acc, value) =>
      for
        paths <- acc
        path <- path(value, "input")
        _ <- Either.cond(
          Files.isDirectory(path) ||
            (Files.isRegularFile(path) && path.getFileName.toString.toLowerCase.endsWith(".gpx")),
          (),
          s"Positional input must be an existing .gpx file or directory, got: $path"
        )
      yield paths :+ path
    }

object Inputs:
  def defaultTrailInputs(): Vector[Path] =
    val dir = Path.of("trails")
    if !Files.isDirectory(dir) then
      Vector.empty
    else
      val stream = Files.list(dir)
      try
        stream.iterator().asScala
          .filter(p => Files.isRegularFile(p))
          .filter(p =>
            p.getFileName.toString.toLowerCase.endsWith(".gpx")
          )
          .toVector
          .sortBy(_.toString)
      finally
        stream.close()

  def expand(paths: Vector[Path]): Vector[Path] =
    paths
      .flatMap(expandOne)
      .map(_.toAbsolutePath.normalize)
      .distinct
      .sortBy(_.toString)

  private def expandOne(path: Path): Vector[Path] =
    if Files.isRegularFile(path) then
      Vector(path)
    else if Files.isDirectory(path) then
      // A directory argument denotes one mandatory-trail root only.
      // Roles below it (avoid/real/ignore) are separate datasets and must
      // never become mandatory through recursive discovery.
      val stream = Files.list(path)
      try
        stream.iterator().asScala
          .filter(p => Files.isRegularFile(p))
          .filter(p => p.getFileName.toString.toLowerCase.endsWith(".gpx"))
          .toVector
      finally stream.close()
    else
      sys.error(s"Input does not exist: $path")

object ForbiddenTransferInputs:
  // GPXs here describe corridors that connectors must not substantially reuse.
  // They are deliberately NOT part of the mandatory technical-trail set.
  def defaultPaths(): Vector[Path] =
    val dir = Path.of("trails", "avoid")
    if !Files.isDirectory(dir) then
      Vector.empty
    else
      val stream = Files.walk(dir)
      try
        stream.iterator().asScala
          .filter(p => Files.isRegularFile(p))
          .filter { p =>
              p.getFileName.toString
                .toLowerCase
                .endsWith(".gpx")
          }
          .map(_.toAbsolutePath.normalize)
          .toVector
          .distinct
          .sortBy(_.toString)
      finally
        stream.close()

object Gpx:
  def read(path: Path): Trail =
    require(Files.isRegularFile(path), s"GPX file not found: $path")
    val dbf = DocumentBuilderFactory.newInstance()
    dbf.setNamespaceAware(true)
    Try(dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true))
    Try(dbf.setFeature("http://xml.org/sax/features/external-general-entities", false))
    Try(dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false))
    Try(dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false))
    val in = FileInputStream(path.toFile)
    try
      val doc = dbf.newDocumentBuilder().parse(in)
      val root = doc.getDocumentElement
      val nonEmptyTracks =
        elementsByLocalName(
          root,
          "trk"
        ).filter { track =>
            elementsByLocalName(
              track,
              "trkpt"
            ).nonEmpty
        }
      require(
        nonEmptyTracks.size <= 1,
        s"Technical GPX $path contains ${nonEmptyTracks.size} non-empty <trk> tracks. " +
          "Multiple disconnected tracks are unsupported."
      )
      val nonEmptyTrackSegments =
        elementsByLocalName(
          root,
          "trkseg"
        ).filter { segment =>
            elementsByLocalName(
              segment,
              "trkpt"
            ).nonEmpty
        }
      val rootTrackPoints = elementsByLocalName(root, "trkpt")
      val nodes =
        if rootTrackPoints.nonEmpty then
          require(
            nonEmptyTrackSegments.size <= 1,
            s"Technical GPX $path contains ${nonEmptyTrackSegments.size} non-empty <trkseg> segments. " +
              "Multiple disconnected segments are unsupported because silently concatenating them would create an artificial trail segment."
          )
          val selected = nonEmptyTrackSegments.headOption
              .map { segment =>
                  elementsByLocalName(segment, "trkpt")
              }
              .getOrElse(rootTrackPoints)
          selected
        else
          val routes =
            elementsByLocalName(
              root,
              "rte"
            ).filter { route =>
                elementsByLocalName(
                  route,
                  "rtept"
                ).nonEmpty
            }
          require(
            routes.size <= 1,
            s"Technical GPX $path contains ${routes.size} non-empty <rte> routes. " +
              "Multiple disconnected routes are unsupported."
          )
          val rtepts = routes.headOption
              .map { route =>
                  elementsByLocalName(route, "rtept")
              }
              .getOrElse(elementsByLocalName(root, "rtept"))
          if rtepts.nonEmpty then rtepts
          else sys.error(s"No <trkpt> or <rtept> points found in $path")
      val points =
        nodes.zipWithIndex.map {
          case (
                e,
                index
              ) =>
            val lat =
              attr(
                e,
                "lat"
              ).toDouble
            val lon =
              attr(
                e,
                "lon"
              ).toDouble
            val ele =
              childText(
                e,
                "ele"
              ).flatMap(_.toDoubleOption)
            require(
              java.lang.Double.isFinite(
                lat
              ) &&
                lat >= -90.0 &&
                lat <= 90.0,
              s"Invalid latitude at point ${index + 1} in $path: $lat"
            )
            require(
              java.lang.Double.isFinite(
                lon
              ) &&
                lon >= -180.0 &&
                lon <= 180.0,
              s"Invalid longitude at point ${index + 1} in $path: $lon"
            )
            ele.foreach { elevation =>
              require(
                java.lang.Double.isFinite(
                  elevation
                ),
                s"Invalid elevation at point ${index + 1} in $path: $elevation"
              )
            }
            Point(lat, lon, ele)
        }.toVector
      require(
        points.size >= 2,
        s"Need at least 2 points in $path, found ${points.size}"
      )
      val name = firstTrackName(root)
          .filter(_.nonEmpty)
          .getOrElse(stripGpx(path.getFileName.toString))
      Trail(path.toAbsolutePath.normalize, name, points)
    finally in.close()

  def write(path: Path, trackName: String, description: String, points: Vector[Point]): Unit =
    Option(path.getParent).foreach(p => Files.createDirectories(p))
    val out = Files.newOutputStream(path)
    val w = XMLOutputFactory.newFactory().createXMLStreamWriter(out, "UTF-8")
    try
      w.writeStartDocument("UTF-8", "1.0")
      w.writeCharacters("\n")
      w.writeStartElement("gpx")
      w.writeDefaultNamespace("http://www.topografix.com/GPX/1/1")
      w.writeAttribute("version", "1.1")
      w.writeAttribute("creator", "trail-plan.scala")
      w.writeCharacters("\n  ")
      w.writeStartElement("metadata")
      w.writeCharacters("\n    ")
      w.writeStartElement("name")
      w.writeCharacters(trackName)
      w.writeEndElement()
      w.writeCharacters("\n    ")
      w.writeStartElement("desc")
      w.writeCharacters(description)
      w.writeEndElement()
      w.writeCharacters("\n  ")
      w.writeEndElement()
      w.writeCharacters("\n  ")
      w.writeStartElement("trk")
      w.writeCharacters("\n    ")
      w.writeStartElement("name")
      w.writeCharacters(trackName)
      w.writeEndElement()
      w.writeCharacters("\n    ")
      w.writeStartElement("trkseg")
      points.foreach { p =>
        w.writeCharacters("\n      ")
        w.writeStartElement("trkpt")
        w.writeAttribute("lat", f"${p.lat}%.7f")
        w.writeAttribute("lon", f"${p.lon}%.7f")
        p.ele.foreach { e =>
          w.writeCharacters("\n        ")
          w.writeStartElement("ele")
          w.writeCharacters(f"$e%.1f")
          w.writeEndElement()
          w.writeCharacters("\n      ")
        }
        w.writeEndElement()
      }
      w.writeCharacters("\n    ")
      w.writeEndElement()
      w.writeCharacters("\n  ")
      w.writeEndElement()
      w.writeCharacters("\n")
      w.writeEndElement()
      w.writeCharacters("\n")
      w.writeEndDocument()
      w.flush()
    finally
      Try(w.close())
      out.close()

  private def stripGpx(s: String): String =
    if s.toLowerCase.endsWith(".gpx") then s.dropRight(4) else s

  private def firstTrackName(root: Element): Option[String] =
    val tracks = elementsByLocalName(root, "trk")
    tracks.headOption.flatMap(t => childText(t, "name"))
      .orElse {
        val routes = elementsByLocalName(root, "rte")
        routes.headOption.flatMap(r => childText(r, "name"))
      }

  private def elementsByLocalName(root: Element, wanted: String): Vector[Element] =
    val all = root.getElementsByTagName("*")
    (0 until all.getLength).iterator.flatMap { i =>
      all.item(i) match
        case e: Element if localName(e) == wanted => Some(e)
        case _                                     => None
    }.toVector

  private def localName(n: Node): String =
    Option(n.getLocalName).getOrElse {
      val raw = n.getNodeName
      raw.substring(raw.lastIndexOf(':') + 1)
    }

  private def attr(e: Element, name: String): String =
    val value = e.getAttribute(name)
    if value == null || value.isBlank then
      sys.error(s"Missing @$name on <${localName(e)}>")
    value

  private def childText(e: Element, wanted: String): Option[String] =
    val children = e.getChildNodes
    (0 until children.getLength).iterator.flatMap { i =>
      children.item(i) match
        case child: Element if localName(child) == wanted => Some(child)
        case _                                             => None
    }.nextOption().flatMap(child => Option(child.getTextContent).map(_.trim).filter(_.nonEmpty))

case class RealRideSample(point: Point, epochSeconds: Double, speedMps: Option[Double], segmentIndex: Int)

case class RealRide(path: Path, name: String, samples: Vector[RealRideSample])

object RealRideGpx:
  def defaultPaths(): Vector[Path] =
    val dir = Path.of("trails", "real")
    if !Files.isDirectory(dir) then
      Vector.empty
    else
      val stream = Files.list(dir)
      try
        stream.iterator().asScala
          .filter(p => Files.isRegularFile(p))
          .filter { p =>
              p.getFileName.toString
                .toLowerCase
                .endsWith(".gpx")
          }
          .toVector
          .sortBy(_.toString)
      finally
        stream.close()

  def read(path: Path): RealRide =
    require(Files.isRegularFile(path), s"Real ride GPX not found: $path")
    val dbf = DocumentBuilderFactory.newInstance()
    dbf.setNamespaceAware(true)
    Try(dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true))
    Try(dbf.setFeature("http://xml.org/sax/features/external-general-entities", false))
    Try(dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false))
    Try(dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false))
    val in = FileInputStream(path.toFile)
    try
      val doc = dbf.newDocumentBuilder()
          .parse(in)
      val root = doc.getDocumentElement

      // A phone recording may legitimately contain several GPX tracks or
      // track segments after a pause/app restart. Keep all of them as ONE
      // recording (one phone), but preserve each continuity segment explicitly.
      // Downstream trail/transfer matching is forbidden from crossing a segment
      // boundary, so a 20-minute / 900-m recording gap cannot become a fake
      // ridden connector.
      val nonEmptyTracks =
        elementsByLocalName(
          root,
          "trk"
        ).filter { track =>
            elementsByLocalName(
              track,
              "trkpt"
            ).nonEmpty
        }
      val sampleSegments =
        if nonEmptyTracks.nonEmpty then
          nonEmptyTracks.flatMap { track =>
            val trackSegments =
              elementsByLocalName(
                track,
                "trkseg"
              ).filter { segment =>
                  elementsByLocalName(
                    segment,
                    "trkpt"
                  ).nonEmpty
              }
            if trackSegments.nonEmpty then
              trackSegments.map { segment =>
                  elementsByLocalName(segment, "trkpt")
              }
            else
              Vector(
                elementsByLocalName(track, "trkpt")
              ).filter(_.nonEmpty)
          }
        else
          Vector(
            elementsByLocalName(root, "trkpt")
          ).filter(_.nonEmpty)
      val samples =
        val out = Vector.newBuilder[RealRideSample]
        var nextContinuityIndex = 0
        sampleSegments.foreach { trackPoints =>
          var continuityIndex = nextContinuityIndex
          nextContinuityIndex +=
            1
          trackPoints.foreach { e =>
            val lat =
              Option(
                e.getAttribute("lat")
              ).flatMap(
                _.toDoubleOption
              ).filter { value =>
                  java.lang.Double.isFinite(value) &&
                    value >= -90.0 &&
                    value <= 90.0
              }
            val lon =
              Option(
                e.getAttribute("lon")
              ).flatMap(
                _.toDoubleOption
              ).filter { value =>
                  java.lang.Double.isFinite(value) &&
                    value >= -180.0 &&
                    value <= 180.0
              }
            val ele =
              childText(
                e,
                "ele"
              ).flatMap(
                _.toDoubleOption
              ).filter { value =>
                  java.lang.Double.isFinite(value)
              }
            val epoch =
              childText(
                e,
                "time"
              ).flatMap { raw =>
                Try(
                  Instant.parse(raw)
                    .toEpochMilli
                    .toDouble / 1000.0
                ).toOption
                  .filter(java.lang.Double.isFinite)
              }
            val speed =
              descendantAttribute(
                e,
                "meta",
                "s"
              ).flatMap(
                _.toDoubleOption
              ).filter { value =>
                  java.lang.Double.isFinite(value)
              }
            (
              lat,
              lon,
              epoch
            ) match
              case (
                    Some(la),
                    Some(lo),
                    Some(t)
                  ) =>
                out +=
                  RealRideSample(
                    Point(
                      la,
                      lo,
                      ele
                    ),
                    t,
                    speed,
                    continuityIndex
                  )
              case _ =>
                // Dropping an invalid required sample must also break continuity;
                // otherwise the valid samples on either side become a fake bridge.
                continuityIndex = nextContinuityIndex
                nextContinuityIndex +=
                  1
          }
        }
        out.result()
      require(samples.size >= 2, s"Need timestamped track points in real ride GPX $path")
      val name =
        elementsByLocalName(
          root,
          "trk"
        ).headOption
          .flatMap { t =>
              childText(t, "name")
          }
          .filter(_.nonEmpty)
          .getOrElse(path.getFileName.toString)
      RealRide(path.toAbsolutePath.normalize, name, samples)
    finally
      in.close()

  private def elementsByLocalName(root: Element, wanted: String): Vector[Element] =
    val all = root.getElementsByTagName("*")
    (0 until all.getLength).iterator.flatMap { i =>
      all.item(i) match
        case e: Element if localName(e) == wanted => Some(e)
        case _                                     => None
    }.toVector

  private def localName(n: Node): String =
    Option(
      n.getLocalName
    ).getOrElse {
      val raw = n.getNodeName
      raw.substring(raw.lastIndexOf(':') + 1)
    }

  private def childText(e: Element, wanted: String): Option[String] =
    val children = e.getChildNodes
    (0 until children.getLength).iterator.flatMap { i =>
      children.item(i) match
        case child: Element if localName(child) == wanted => Some(child)
        case _                                             => None
    }.nextOption().flatMap(child => Option(child.getTextContent).map(_.trim).filter(_.nonEmpty))

  private def descendantAttribute(e: Element, wantedLocalName: String, attribute: String): Option[String] =
    val all = e.getElementsByTagName("*")
    (0 until all.getLength).iterator.flatMap { i =>
      all.item(i) match
        case child: Element if localName(child) == wantedLocalName =>
          Option(child.getAttribute(attribute)).map(_.trim).filter(_.nonEmpty)
        case _ =>
          None
    }.nextOption()

object Geometry:
  private val EarthRadiusM = 6371000.0

  def distanceMeters(a: Point, b: Point): Double =
    val p1 = math.toRadians(a.lat)
    val p2 = math.toRadians(b.lat)
    val dp = math.toRadians(b.lat - a.lat)
    val dl = math.toRadians(b.lon - a.lon)
    val h = math.sin(dp / 2) * math.sin(dp / 2) +
        math.cos(p1) * math.cos(p2) *
          math.sin(dl / 2) * math.sin(dl / 2)
    2 * EarthRadiusM * math.asin(math.min(1.0, math.sqrt(h)))

  def pathLengthMeters(points: Vector[Point]): Double =
    points.sliding(2).map {
      case Vector(a, b) => distanceMeters(a, b)
      case _            => 0.0
    }.sum

  case class PolylineProjection(alongM: Double, lateralM: Double)

  def projectToPolyline(p: Point, polyline: Vector[Point]): Option[PolylineProjection] =
    if polyline.size < 2 then
      None
    else
      val lat0 = math.toRadians(p.lat)

      def xy(q: Point): (Double, Double) =
        val x =
          math.toRadians(
            q.lon - p.lon
          ) *
            EarthRadiusM *
            math.cos(lat0)
        val y =
          math.toRadians(
            q.lat - p.lat
          ) *
            EarthRadiusM
        (x, y)

      var bestLateral = Double.PositiveInfinity
      var bestAlong = 0.0
      var cumulative = 0.0
      var i = 0
      while i + 1 < polyline.size do
        val a = polyline(i)
        val b = polyline(i + 1)
        val segmentM = distanceMeters(a, b)
        val (ax, ay) =
          xy(a)
        val (bx, by) =
          xy(b)
        val vx = bx - ax
        val vy = by - ay
        val vv = vx * vx +
            vy * vy
        val t =
          if vv <= 1e-9 then
            0.0
          else
            math.max(
              0.0,
              math.min(
                1.0,
                -(
                  ax * vx +
                    ay * vy
                ) / vv
              )
            )
        val dx = ax +
            t * vx
        val dy = ay +
            t * vy
        val lateral = math.hypot(dx, dy)
        if lateral < bestLateral then
          bestLateral = lateral
          bestAlong =
            cumulative +
              t * segmentM
        cumulative +=
          segmentM
        i += 1
      Some(PolylineProjection(bestAlong, bestLateral))

  def pointsWithCumulativeDistance(points: Vector[Point]): Vector[(Double, Point)] =
    if points.isEmpty then
      Vector.empty
    else
      val out = Vector.newBuilder[(Double, Point)]
      var cumulative = 0.0
      out +=
        ((
          0.0,
          points.head
        ))
      var i = 1
      while i < points.size do
        cumulative +=
          distanceMeters(points(i - 1), points(i))
        out +=
          ((
            cumulative,
            points(i)
          ))
        i += 1
      out.result()

  // Horizontal winding of the supplied trail geometry. 1.0 is straight;
  // values above 1 mean the ridden line is longer than its start/end chord.
  // Rider speed is deliberately irrelevant.
  def sinuosity(points: Vector[Point]): Double =
    if points.size < 2 then 1.0
    else
      val length = pathLengthMeters(points)
      val chord = distanceMeters(points.head, points.last)
      if length <= 1e-9 then 1.0
      else if chord <= 1e-6 then Double.PositiveInfinity
      else length / chord

  case class DescentSinuosityWindow(centerM: Double, distanceM: Double, netDescentPct: Double, sinuosity: Double)

  private def pointAtPathDistance(profile: Vector[(Double, Point)], targetM: Double): Option[Point] =
    if profile.isEmpty ||
        targetM < -1e-9 ||
        targetM > profile.last._1 + 1e-9
    then
      None
    else if targetM <= 0.0 then
      Some(profile.head._2)
    else if targetM >= profile.last._1 then
      Some(profile.last._2)
    else
      var low = 0
      var high = profile.size - 1
      while high - low > 1 do
        val mid =
          (
            low +
              high
          ) /
            2
        if profile(mid)._1 <= targetM
        then
          low = mid
        else
          high = mid
      val (
        d0,
        p0
      ) =
        profile(low)
      val (
        d1,
        p1
      ) =
        profile(high)
      val span = d1 -
          d0
      if span <= 1e-12 then
        Some(p0)
      else
        val t =
          math.max(
            0.0,
            math.min(
              1.0,
              (
                targetM -
                  d0
              ) /
                span
            )
          )
        val ele =
          (
            p0.ele,
            p1.ele
          ) match
            case (
                  Some(a),
                  Some(b)
                ) =>
              Some(
                a +
                  (
                    b -
                      a
                  ) *
                    t
              )
            case _ =>
              None
        Some(
          Point(
            lat =
              p0.lat +
                (
                  p1.lat -
                    p0.lat
                ) *
                  t,
            lon =
              p0.lon +
                (
                  p1.lon -
                    p0.lon
                ) *
                  t,
            ele = ele
          )
        )

  private def localWindowAtStart(profile: Vector[(Double, Point)], startM: Double, windowM: Double): Option[DescentSinuosityWindow] =
    for
      start <-
        pointAtPathDistance(profile, startM)
      end <-
        pointAtPathDistance(
          profile,
          startM +
            windowM
        )
    yield
      val netDescentPct =
        (
          start.ele,
          end.ele
        ) match
          case (
                Some(a),
                Some(b)
              ) =>
            math.max(
              0.0,
              a -
                b
            ) /
              windowM *
              100.0
          case _ =>
            0.0
      val chordM = distanceMeters(start, end)
      val localSinuosity =
        if chordM <= 1e-6 then Double.PositiveInfinity
        else windowM /
            chordM
      DescentSinuosityWindow(
        centerM =
          startM +
            windowM /
              2.0,
        distanceM = windowM,
        netDescentPct = netDescentPct,
        sinuosity = localSinuosity
      )

  private def uniqueSortedDistances(values: Vector[Double], epsilonM: Double = 1e-6): Vector[Double] =
    values.sorted.foldLeft(
      Vector.empty[Double]
    ) {
      case (
            acc,
            value
          ) =>
        if acc.isEmpty ||
            math.abs(
              acc.last -
                value
            ) >
              epsilonM
        then
          acc :+
            value
        else
          acc
    }

  private def minimizeWindowChordStart(profile: Vector[(Double, Point)], windowM: Double, leftM: Double, rightM: Double): Double =
    if rightM -
        leftM <=
        1e-6
    then
      (
        leftM +
          rightM
      ) /
        2.0
    else
      def chord(startM: Double): Double =
        (
          pointAtPathDistance(
            profile,
            startM
          ),
          pointAtPathDistance(
            profile,
            startM +
              windowM
          )
        ) match
          case (
                Some(a),
                Some(b)
              ) =>
            distanceMeters(a, b)
          case _ =>
            Double.PositiveInfinity

      val golden =
        (
          math.sqrt(
            5.0
          ) -
            1.0
        ) /
          2.0
      var a = leftM
      var b = rightM
      var x1 = b -
          golden *
            (
              b -
                a
            )
      var x2 = a +
          golden *
            (
              b -
                a
            )
      var f1 = chord(x1)
      var f2 = chord(x2)
      var iteration = 0
      while iteration < 48 &&
          b -
            a >
            1e-4
      do
        if f1 <= f2 then
          b = x2
          x2 = x1
          f2 = f1
          x1 =
            b -
              golden *
                (
                  b -
                    a
                )
          f1 = chord(x1)
        else
          a = x1
          x1 = x2
          f1 = f2
          x2 =
            a +
              golden *
                (
                  b -
                    a
                )
          f2 = chord(x2)
        iteration +=
          1
      (
        a +
          b
      ) /
        2.0

  private def bisectWindowChordThreshold(
      profile: Vector[(Double, Point)],
      windowM: Double,
      targetChordM: Double,
      leftM: Double,
      rightM: Double
  ): Option[Double] =
    def value(startM: Double): Option[Double] =
      for
        a <-
          pointAtPathDistance(profile, startM)
        b <-
          pointAtPathDistance(
            profile,
            startM +
              windowM
          )
      yield
        distanceMeters(
          a,
          b
        ) -
          targetChordM
    (
      value(
        leftM
      ),
      value(rightM)
    ) match
      case (Some(leftValue), Some(rightValue))
          if java.lang.Double.isFinite(
              leftValue
            ) &&
            java.lang.Double.isFinite(
              rightValue
            ) &&
            leftValue *
              rightValue <=
              0.0 =>
        var a = leftM
        var b = rightM
        var fa = leftValue
        var iteration = 0
        while iteration < 56 &&
            b -
              a >
              1e-4
        do
          val mid =
            (
              a +
                b
            ) /
              2.0
          val fm =
            value(
              mid
            ).getOrElse(Double.NaN)
          if !java.lang.Double.isFinite(fm)
          then
            return None
          if fa *
              fm <=
              0.0
          then
            b = mid
          else
            a = mid
            fa = fm
          iteration +=
            1
        Some(
          (
            a +
              b
          ) /
            2.0
        )
      case _ =>
        None

  def descentSinuosityWindows(
      points: Vector[Point],
      windowM: Double,
      minNetDescentPct: Double,
      minSinuosity: Double
  ): Vector[DescentSinuosityWindow] =
    if points.size < 2 ||
        windowM <= 0.0 ||
        minNetDescentPct < 0.0 ||
        minSinuosity <= 0.0
    then
      Vector.empty
    else
      val dense = resample(points, 5.0)
      val profile = pointsWithCumulativeDistance(dense)
      if profile.size < 2 ||
          profile.last._1 <
            windowM
      then
        Vector.empty
      else
        val totalM = profile.last._1
        val maxStartM = totalM -
            windowM
        val critical =
          uniqueSortedDistances(
            (
              Vector(
                0.0,
                maxStartM
              ) ++
                profile.flatMap {
                  case (
                        distanceM,
                        _
                      ) =>
                    Vector(
                      distanceM,
                      distanceM -
                        windowM
                    )
                }
            ).filter(
              value =>
                value >= -1e-9 &&
                  value <= maxStartM + 1e-9
            ).map { value =>
                math.max(0.0, math.min(maxStartM, value))
            }
          )
        val candidateStarts = mutable.ArrayBuffer[Double]()
        candidateStarts ++=
          critical
        val targetDescentM = minNetDescentPct /
            100.0 *
            windowM
        val targetChordM = windowM /
            minSinuosity
        critical.sliding(2).foreach {
          case Vector(leftM, rightM)
              if rightM >
                leftM + 1e-9 =>
            val leftWindow = localWindowAtStart(profile, leftM, windowM)
            val rightWindow = localWindowAtStart(profile, rightM, windowM)

            // Net descent is linear inside a critical interval. Add the exact
            // threshold crossing, if any, so the conjunction with sinuosity
            // cannot be missed merely because the crossing lies off-grid.
            (
              leftWindow,
              rightWindow
            ) match
              case (
                    Some(left),
                    Some(right)
                  ) =>
                val leftDescentM = left.netDescentPct /
                    100.0 *
                    windowM
                val rightDescentM = right.netDescentPct /
                    100.0 *
                    windowM
                val leftDelta = leftDescentM -
                    targetDescentM
                val rightDelta = rightDescentM -
                    targetDescentM
                if leftDelta *
                    rightDelta <
                    0.0 &&
                    math.abs(
                      rightDescentM -
                        leftDescentM
                    ) >
                      1e-12
                then
                  val t =
                    (
                      targetDescentM -
                        leftDescentM
                    ) /
                      (
                        rightDescentM -
                          leftDescentM
                      )
                  candidateStarts +=
                    leftM +
                      (
                        rightM -
                          leftM
                      ) *
                        t
              case _ =>
            val minimumChordStart = minimizeWindowChordStart(profile, windowM, leftM, rightM)
            candidateStarts +=
              minimumChordStart

            def chordDelta(startM: Double): Option[Double] =
              for
                start <-
                  pointAtPathDistance(profile, startM)
                end <-
                  pointAtPathDistance(
                    profile,
                    startM +
                      windowM
                  )
              yield
                distanceMeters(
                  start,
                  end
                ) -
                  targetChordM

            val leftChordDelta = chordDelta(leftM)
            val minimumChordDelta = chordDelta(minimumChordStart)
            val rightChordDelta = chordDelta(rightM)
            (
              leftChordDelta,
              minimumChordDelta
            ) match
              case (Some(a), Some(b))
                  if a *
                      b <
                      0.0 =>
                bisectWindowChordThreshold(
                  profile,
                  windowM,
                  targetChordM,
                  leftM,
                  minimumChordStart
                ).foreach(
                  candidateStarts +=
                    _
                )
              case _ =>
            (
              minimumChordDelta,
              rightChordDelta
            ) match
              case (Some(a), Some(b))
                  if a *
                      b <
                      0.0 =>
                bisectWindowChordThreshold(
                  profile,
                  windowM,
                  targetChordM,
                  minimumChordStart,
                  rightM
                ).foreach(
                  candidateStarts +=
                    _
                )
              case _ =>
          case _ =>
        }
        uniqueSortedDistances(
          candidateStarts.toVector
        ).flatMap { startM =>
            localWindowAtStart(profile, startM, windowM)
        }

  def ascentDescent(points: Vector[Point]): (Double, Double) =
    points.sliding(2).foldLeft((0.0, 0.0)) {
      case ((up, down), Vector(a, b)) =>
        (a.ele, b.ele) match
          case (Some(x), Some(y)) if y >= x => (up + y - x, down)
          case (Some(x), Some(y))           => (up, down + x - y)
          case _                            => (up, down)
      case (totals, _) =>
        totals
    }

  def exponentiallyWeightedAscentNearEnd(points: Vector[Point], decayM: Double): Double =
    if decayM <= 0.0 || points.size < 2 then 0.0
    else
      points.sliding(2).toVector.reverse.foldLeft((0.0, 0.0)) {
        case ((weighted, distanceFromEndM), Vector(a, b)) =>
          val segmentM = distanceMeters(a, b)
          if segmentM <= 0.0 then
            (weighted, distanceFromEndM)
          else
            val added = (a.ele, b.ele) match
                case (Some(z0), Some(z1)) if z1 > z0 =>
                  val midpointFromEndM = distanceFromEndM + segmentM * 0.5
                  (z1 - z0) * math.exp(-midpointFromEndM / decayM)
                case _ =>
                  0.0
            (weighted + added, distanceFromEndM + segmentM)
        case (state, _) =>
          state
      }._1
  private case class GradeChunk(
      distanceM: Double,
      deltaM: Double
  ):
    def grade: Double =
      if distanceM > 0.0 then deltaM / distanceM else 0.0

  def resample(points: Vector[Point], spacingM: Double): Vector[Point] =
    if points.size < 2 || spacingM <= 0.0 then points
    else
      val generated =
        points.sliding(2).flatMap {
          case Vector(a, b) =>
            val segmentM = distanceMeters(a, b)
            if segmentM <= 0.0 then Iterator.empty
            else
              val steps = math.max(1, math.ceil(segmentM / spacingM).toInt)
              (1 to steps).iterator.map { k =>
                val t = k.toDouble / steps.toDouble
                val ele = (a.ele, b.ele) match
                    case (Some(x), Some(y)) => Some(x + (y - x) * t)
                    case _                  => None
                Point(lat = a.lat + (b.lat - a.lat) * t, lon = a.lon + (b.lon - a.lon) * t, ele = ele)
              }
          case _ =>
            Iterator.empty
        }.toVector
      val result = points.head +: generated
      if result.size >= 2 &&
          distanceMeters(result(result.size - 2), result.last) < 0.05
      then result.dropRight(1) :+ points.last
      else result

  def profileStats(points: Vector[Point]): ProfileStats =
    val length = pathLengthMeters(points)
    val (ascent, descent) = ascentDescent(points)
    val startEle = points.headOption.flatMap(_.ele)
    val endEle = points.lastOption.flatMap(_.ele)
    val net =
      for
        a <- startEle
        b <- endEle
      yield b - a
    val chunks = gradeChunks(points, 20.0)
    val (bestClimbDistance, bestClimbGain, _, _) =
      chunks.foldLeft((0.0, 0.0, 0.0, 0.0)) {
        case ((bestDistance, bestGain, runDistance, runGain), chunk)
            if chunk.grade > 0.01 =>
          val nextRunDistance = runDistance + chunk.distanceM
          val nextRunGain = runGain + math.max(0.0, chunk.deltaM)
          if nextRunDistance > bestDistance then (nextRunDistance, nextRunGain, nextRunDistance, nextRunGain)
          else (bestDistance, bestGain, nextRunDistance, nextRunGain)
        case ((bestDistance, bestGain, _, _), _) =>
          (bestDistance, bestGain, 0.0, 0.0)
      }
    val longestGrade =
      if bestClimbDistance > 0.0 then
        bestClimbGain / bestClimbDistance * 100.0
      else 0.0
    val maxGap =
      points.sliding(2).map {
        case Vector(a, b) => distanceMeters(a, b)
        case _            => 0.0
      }.foldLeft(0.0)(math.max)
    ProfileStats(
      lengthM = length,
      ascentM = ascent,
      descentM = descent,
      netElevationM = net,
      longestClimbDistanceM = bestClimbDistance,
      longestClimbAvgGradePct = longestGrade,
      maxGrade30Pct = maxSustainedGradePct(points, 30.0),
      maxGrade100Pct = maxSustainedGradePct(points, 100.0),
      maxPointGapM = maxGap
    )

  private def gradeChunks(points: Vector[Point], targetM: Double): Vector[GradeChunk] =
    if points.size < 2 then Vector.empty
    else
      val (out, _, _) =
        points.sliding(2).zipWithIndex.foldLeft(
          (Vector.newBuilder[GradeChunk], points.head, 0.0)
        ) {
          case ((out, anchor, accumulatedM), (Vector(previous, current), index)) =>
            val nextAccumulatedM = accumulatedM + distanceMeters(previous, current)
            val flush = nextAccumulatedM >= targetM || index == points.size - 2
            if flush && nextAccumulatedM > 0.0 then
              (anchor.ele, current.ele) match
                case (Some(z0), Some(z1)) =>
                  out += GradeChunk(nextAccumulatedM, z1 - z0)
                case _ =>
              (out, current, 0.0)
            else
              (out, anchor, nextAccumulatedM)
          case (state, _) =>
            state
        }
      out.result()

  // For a piecewise-linear elevation profile, g(s)=z(s+W)-z(s) is itself
  // piecewise linear. Its slope can change only when s or s+W crosses an
  // elevation-profile breakpoint. Therefore the true fixed-window extrema are
  // attained at:
  //   0, L-W, every breakpoint d, or every shifted breakpoint d-W
  // that lies in [0, L-W].
  //
  // Evaluating exactly this finite set removes the old 10 m phase hole from
  // the 30/100 m hard wall envelope.
  private def criticalWindowStarts(profile: Vector[(Double, Double)], windowM: Double): Vector[Double] =
    if profile.size < 2 ||
        windowM <= 0.0 ||
        profile.last._1 < windowM
    then
      Vector.empty
    else
      val maxStart = profile.last._1 - windowM
      val starts = mutable.ArrayBuffer[Double](0.0, maxStart)
      profile.foreach {
        case (
              distanceM,
              _
            ) =>
          if distanceM >= -1e-9 &&
              distanceM <= maxStart + 1e-9
          then
            starts +=
              math.max(0.0, math.min(maxStart, distanceM))
          val shifted = distanceM - windowM
          if shifted >= -1e-9 &&
              shifted <= maxStart + 1e-9
          then
            starts +=
              math.max(0.0, math.min(maxStart, shifted))
      }
      starts.toVector
        .distinct
        .sorted

  // Continuous, phase-independent soft handling exposure.
  //
  // For a fixed window W on a piecewise-linear elevation profile,
  // descent(s)=z(s)-z(s+W) is piecewise linear between the same critical
  // starts used by the hard wall/downhill extrema. Within each interval,
  // the set where the 30 m descent meets a handling threshold is therefore
  // an interval that can be solved exactly by linear interpolation.
  //
  // Return the union of ACTUAL ridden distance covered by qualifying windows,
  // not old 0/10/20/... sample centers.
  def sustainedDescentCoverageIntervals(points: Vector[Point], windowM: Double, minDescentPct: Double): Vector[(Double, Double)] =
    val profile = elevationProfile(points)
    val starts = criticalWindowStarts(profile, windowM)
    if starts.isEmpty ||
        windowM <= 0.0 ||
        minDescentPct < 0.0
    then
      Vector.empty
    else
      def descentPctAt(startM: Double): Option[Double] =
        for
          a <-
            elevationAt(profile, startM)
          b <-
            elevationAt(profile, startM + windowM)
        yield
          (
            a -
              b
          ) /
            windowM *
            100.0

      val qualifyingStarts = mutable.ArrayBuffer[(Double, Double)]()

      // An exactly-threshold critical window still represents a real W-metre
      // technical section even if the threshold is touched only at a kink.
      starts.foreach { startM =>
        descentPctAt(
          startM
        ).foreach { descentPct =>
          if descentPct + 1e-12 >= minDescentPct
          then
            qualifyingStarts += ((
              startM,
              startM
            ))
        }
      }
      starts.sliding(2).foreach {
        case Vector(leftM, rightM)
            if rightM >
              leftM + 1e-12 =>
          (
            descentPctAt(
              leftM
            ),
            descentPctAt(rightM)
          ) match
            case (
                  Some(leftPct),
                  Some(rightPct)
                ) =>
              val leftQualifies =
                leftPct + 1e-12 >= minDescentPct
              val rightQualifies =
                rightPct + 1e-12 >= minDescentPct
              if leftQualifies &&
                  rightQualifies
              then
                qualifyingStarts += ((
                  leftM,
                  rightM
                ))
              else if leftQualifies != rightQualifies
              then
                val delta = rightPct -
                    leftPct
                if math.abs(
                    delta
                  ) >
                    1e-12
                then
                  val t =
                    (
                      minDescentPct -
                        leftPct
                    ) /
                      delta
                  val crossingM =
                    math.max(
                      leftM,
                      math.min(
                        rightM,
                        leftM +
                          (
                            rightM -
                              leftM
                          ) *
                            t
                      )
                    )
                  if leftQualifies then
                    qualifyingStarts += ((
                      leftM,
                      crossingM
                    ))
                  else
                    qualifyingStarts += ((
                      crossingM,
                      rightM
                    ))
            case _ =>
        case _ =>
      }
      val covered = qualifyingStarts.toVector
          .map {
            case (
                  startM,
                  endStartM
                ) =>
              (
                startM,
                endStartM +
                  windowM
              )
          }
          .sortBy(_._1)
      covered.foldLeft(
        Vector.empty[(Double, Double)]
      ) {
        case (
              acc,
              (startM, endM)
            ) =>
          acc.lastOption match
            case Some((previousStartM, previousEndM))
                if startM <= previousEndM + 1e-6 =>
              acc.dropRight(
                1
              ) :+
                (previousStartM, math.max(previousEndM, endM))
            case _ =>
              acc :+
                (startM, endM)
      }

  def exactSustainedGradeWindows(points: Vector[Point], windowM: Double): Vector[(Double, Double)] =
    val profile = elevationProfile(points)
    criticalWindowStarts(
      profile,
      windowM
    ).flatMap { startM =>
      for
        a <-
          elevationAt(profile, startM)
        b <-
          elevationAt(profile, startM + windowM)
      yield
        (startM + windowM / 2.0, (b - a) / windowM * 100.0)
    }

  def maxSustainedGradePct(points: Vector[Point], windowM: Double): Double =
    exactSustainedGradeWindows(
      points,
      windowM
    ).map(
      _._2
    ).foldLeft(
      0.0
    )(math.max)

  def maxSustainedDescentPct(points: Vector[Point], windowM: Double): Double =
    exactSustainedGradeWindows(
      points,
      windowM
    ).map {
      case (
            _,
            gradePct
          ) =>
        math.max(0.0, -gradePct)
    }.foldLeft(
      0.0
    )(math.max)

  private def elevationProfile(points: Vector[Point]): Vector[(Double, Double)] =
    if points.isEmpty then Vector.empty
    else
      val cumulativeDistances =
        points.sliding(2).scanLeft(0.0) {
          case (cumulative, Vector(a, b)) =>
            cumulative + distanceMeters(a, b)
          case (cumulative, _) =>
            cumulative
        }
      points.iterator.zip(cumulativeDistances).flatMap {
        case (point, cumulativeM) =>
          point.ele.map(elevation => (cumulativeM, elevation))
      }.toVector

  private def elevationAt(profile: Vector[(Double, Double)], distanceM: Double): Option[Double] =
    if profile.isEmpty then None
    else if distanceM <= profile.head._1 then Some(profile.head._2)
    else if distanceM >= profile.last._1 then Some(profile.last._2)
    else
      profile.sliding(2).collectFirst {
        case Vector((d0, z0), (d1, z1))
            if distanceM >= d0 && distanceM <= d1 =>
          val span = d1 - d0
          if span <= 0.0 then z1
          else
            val t = (distanceM - d0) / span
            z0 + (z1 - z0) * t
      }

  def sampleMatchDirection(output: Vector[Point], trail: Vector[Point], toleranceM: Double = 3.0): (Boolean, Boolean) =
    if output.isEmpty || trail.size < 3 then (false, false)
    else
      // Use more than head/mid/end and require one compact occurrence.
      // This prevents a later endpoint-area revisit of trail.head from being
      // combined with mid/end samples from the earlier full trail ride.
      val wantedSamples = math.min(7, trail.size)
      val sampleIndices = (0 until wantedSamples)
          .map { k =>
            math.round(
              k.toDouble *
                (trail.size - 1).toDouble /
                math.max(1, wantedSamples - 1).toDouble
            ).toInt
          }
          .distinct
          .toVector
      val samples = sampleIndices.map(trail)
      val positions = samples.map { sample =>
          output.indices.filter { i =>
            distanceMeters(output(i), sample) <= toleranceM
          }.toVector
        }
      if positions.exists(_.isEmpty) then
        (false, false)
      else
        val cumulative =
          output.sliding(2).scanLeft(0.0) {
            case (cumulativeM, Vector(a, b)) =>
              cumulativeM + distanceMeters(a, b)
            case (cumulativeM, _) =>
              cumulativeM
          }.toVector
        val trailLengthM = pathLengthMeters(trail)
        val minOccurrenceSpanM = math.max(5.0, trailLengthM * 0.55)
        val maxOccurrenceSpanM = math.max(minOccurrenceSpanM + 20.0, trailLengthM * 1.65 + 50.0)

        def occurrenceExists(increasing: Boolean): Boolean =
          def ordered(next: Int, prev: Int): Boolean =
            if increasing then next > prev
            else next < prev

          def search(sampleIdx: Int, prevPos: Int, firstPos: Int): Boolean =
            if sampleIdx >= positions.size then
              val spanM =
                math.abs(
                  cumulative(prevPos) -
                    cumulative(firstPos)
                )
              spanM >= minOccurrenceSpanM &&
                spanM <= maxOccurrenceSpanM
            else
              positions(sampleIdx).exists { pos =>
                if !ordered(pos, prevPos) then
                  false
                else
                  val partialSpanM =
                    math.abs(
                      cumulative(pos) -
                        cumulative(firstPos)
                    )
                  partialSpanM <= maxOccurrenceSpanM &&
                    search(sampleIdx + 1, pos, firstPos)
              }
          positions.head.exists { firstPos =>
            search(sampleIdx = 1, prevPos = firstPos, firstPos = firstPos)
          }
        (occurrenceExists(increasing = true), occurrenceExists(increasing = false))

  /**
   * Parameter intervals of route segment AB that lie inside the tolerance
   * tube around a corridor polyline. Each corridor segment contributes an
   * exact local-plane capsule (rectangle + endpoint circles); all intervals
   * are unioned.
   */
  private def segmentInsideCorridorTubeIntervals(a: Point, b: Point, corridor: Vector[Point], radiusM: Double): Vector[(Double, Double)] =
    if corridor.size < 2 || radiusM < 0.0 then Vector.empty
    else
      val lat0 = math.toRadians((a.lat + b.lat) / 2.0)

      def xy(p: Point): (Double, Double) =
        (math.toRadians(p.lon - a.lon) * EarthRadiusM * math.cos(lat0), math.toRadians(p.lat - a.lat) * EarthRadiusM)

      val (rvx, rvy) = xy(b)
      val routeVV = rvx * rvx + rvy * rvy
      if routeVV <= 1e-12 then Vector.empty
      else
        val routeMinX = math.min(0.0, rvx)
        val routeMaxX = math.max(0.0, rvx)
        val routeMinY = math.min(0.0, rvy)
        val routeMaxY = math.max(0.0, rvy)
        val raw = mutable.ArrayBuffer.empty[(Double, Double)]

        def add(lo0: Double, hi0: Double): Unit =
          val lo = math.max(0.0, math.min(1.0, math.min(lo0, hi0)))
          val hi = math.max(0.0, math.min(1.0, math.max(lo0, hi0)))
          if hi > lo + 1e-12 then raw += ((lo, hi))

        def band(v0: Double, dv: Double, lo: Double, hi: Double): Option[(Double, Double)] =
          if math.abs(dv) <= 1e-12 then
            if v0 >= lo && v0 <= hi then Some((0.0, 1.0)) else None
          else
            val t1 = (lo - v0) / dv
            val t2 = (hi - v0) / dv
            val a = math.max(0.0, math.min(t1, t2))
            val b = math.min(1.0, math.max(t1, t2))
            if b > a + 1e-12 then Some((a, b)) else None

        def addCircle(cx: Double, cy: Double): Unit =
          val bb = -2.0 * (cx * rvx + cy * rvy)
          val cc = cx * cx + cy * cy - radiusM * radiusM
          val disc = bb * bb - 4.0 * routeVV * cc
          if disc >= 0.0 then
            val root = math.sqrt(math.max(0.0, disc))
            add((-bb - root) / (2.0 * routeVV), (-bb + root) / (2.0 * routeVV))

        var i = 0
        while i + 1 < corridor.size do
          val (cx, cy) = xy(corridor(i))
          val (dx, dy) = xy(corridor(i + 1))
          val svx = dx - cx
          val svy = dy - cy
          val segM = math.hypot(svx, svy)
          val tubeMinX =
            math.min(
              cx,
              dx
            ) -
              radiusM
          val tubeMaxX =
            math.max(
              cx,
              dx
            ) +
              radiusM
          val tubeMinY =
            math.min(
              cy,
              dy
            ) -
              radiusM
          val tubeMaxY =
            math.max(
              cy,
              dy
            ) +
              radiusM
          val aabbCouldIntersect = routeMaxX >= tubeMinX &&
              routeMinX <= tubeMaxX &&
              routeMaxY >= tubeMinY &&
              routeMinY <= tubeMaxY
          if aabbCouldIntersect then
            addCircle(cx, cy)
            addCircle(dx, dy)
            if segM > 1e-9 then
              val ux = svx / segM
              val uy = svy / segM
              val nx = -uy
              val ny = ux
              val along0 = -(cx * ux + cy * uy)
              val alongDv = rvx * ux + rvy * uy
              val lateral0 = -(cx * nx + cy * ny)
              val lateralDv = rvx * nx + rvy * ny
              (
                band(
                  along0,
                  alongDv,
                  0.0,
                  segM
                ),
                band(lateral0, lateralDv, -radiusM, radiusM)
              ) match
                case (
                      Some((aLo, aHi)),
                      Some((bLo, bHi))
                    ) =>
                  val intersectionLo =
                    math.max(
                      aLo,
                      bLo
                    )
                  val intersectionHi =
                    math.min(aHi, bHi)
                  if intersectionHi > intersectionLo + 1e-12 then
                    add(
                      intersectionLo,
                      intersectionHi
                    )
                case _ =>
          i += 1
        val sorted = raw.toVector.sortBy(_._1)
        if sorted.isEmpty then Vector.empty
        else
          val merged = Vector.newBuilder[(Double, Double)]
          var lo = sorted.head._1
          var hi = sorted.head._2
          var j = 1
          while j < sorted.size do
            val (nextLo, nextHi) = sorted(j)
            if nextLo <= hi + 1e-10 then hi = math.max(hi, nextHi)
            else
              merged += ((lo, hi))
              lo = nextLo
              hi = nextHi
            j += 1
          merged += ((lo, hi))
          merged.result()

  private def interpolatePoint(a: Point, b: Point, t0: Double): Point =
    val t = math.max(0.0, math.min(1.0, t0))
    Point(a.lat + (b.lat - a.lat) * t, a.lon + (b.lon - a.lon) * t)

  /**
   * Signed nearest-corridor lateral distance. Used only to distinguish a
   * simple side-to-side crossing from travel mainly along the corridor.
   */
  private def signedLateralToPolylineMeters(p: Point, corridor: Vector[Point]): Option[Double] =
    if corridor.size < 2 then None
    else
      val lat0 = math.toRadians(p.lat)

      def xy(q: Point): (Double, Double) =
        (math.toRadians(q.lon - p.lon) * EarthRadiusM * math.cos(lat0), math.toRadians(q.lat - p.lat) * EarthRadiusM)

      val (best, signed) =
        corridor.sliding(2).foldLeft((Double.PositiveInfinity, 0.0)) {
          case ((best, signed), Vector(a, b)) =>
            val (ax, ay) = xy(a)
            val (bx, by) = xy(b)
            val vx = bx - ax
            val vy = by - ay
            val vv = vx * vx + vy * vy
            if vv <= 1e-12 then
              (best, signed)
            else
              val t = math.max(0.0, math.min(1.0, -(ax * vx + ay * vy) / vv))
              val qx = ax + t * vx
              val qy = ay + t * vy
              val lateral = math.hypot(qx, qy)
              if lateral < best then (lateral, (vy * qx - vx * qy) / math.sqrt(vv))
              else (best, signed)
          case (state, _) =>
            state
        }
      if best.isInfinite then None else Some(signed)

  /**
   * Hard /avoid co-travel measurement.
   *
   * Every route segment is clipped against the corridor tube, so entering or
   * leaving through the 12 m boundary no longer discards the in-tube part.
   * Reverse travel counts. A chord that simply crosses from one side to the
   * other is ignored when its across-corridor sweep is at least its progress
   * along the corridor.
   */
  private case class CoTravelChunk(meters: Double, midpoint: Point)

  /**
   * Qualifying pieces of route that actually co-travel with the corridor.
   * This is the single geometric definition used both by the hard audit and
   * by route-derived Valhalla blockers.
   */
  private def coTravelChunks(
      route: Vector[Point],
      corridor: Vector[Point],
      toleranceM: Double,
      minAlongFraction: Double
  ): Vector[CoTravelChunk] =
    if route.size < 2 || corridor.size < 2 then
      Vector.empty
    else
      val out = Vector.newBuilder[CoTravelChunk]
      var i = 0
      while i + 1 < route.size do
        val a = route(i)
        val b = route(i + 1)
        val segmentM = distanceMeters(a, b)
        if segmentM > 0.0 then
          segmentInsideCorridorTubeIntervals(
            a,
            b,
            corridor,
            toleranceM
          ).foreach {
            case (
                  lo,
                  hi
                ) =>
              val clippedM = segmentM *
                  (
                    hi -
                      lo
                  )
              if clippedM > 1e-9 then
                val clippedA = interpolatePoint(a, b, lo)
                val clippedB = interpolatePoint(a, b, hi)
                (
                  projectToPolyline(
                    clippedA,
                    corridor
                  ),
                  projectToPolyline(clippedB, corridor)
                ) match
                  case (
                        Some(pa),
                        Some(pb)
                      ) =>
                    val alongDeltaM =
                      math.abs(
                        pb.alongM -
                          pa.alongM
                      )
                    val alongFraction = alongDeltaM /
                        clippedM
                    val plausible =
                      alongDeltaM <= clippedM * 1.8 +
                          5.0
                    val crossingLike =
                      (
                        signedLateralToPolylineMeters(
                          clippedA,
                          corridor
                        ),
                        signedLateralToPolylineMeters(clippedB, corridor)
                      ) match
                        case (
                              Some(sa),
                              Some(sb)
                            ) =>
                          sa *
                              sb <
                            0.0 &&
                            math.abs(
                              sb -
                                sa
                            ) >= alongDeltaM -
                                1e-6
                        case _ =>
                          false
                    if !crossingLike &&
                        alongFraction >= minAlongFraction &&
                        plausible
                    then
                      out +=
                        CoTravelChunk(
                          meters = clippedM,
                          midpoint =
                            interpolatePoint(
                              a,
                              b,
                              (
                                lo +
                                  hi
                              ) /
                                2.0
                            )
                        )
                  case _ =>
          }
        i +=
          1
      out.result()

  def coTravelOverlapMeters(route: Vector[Point], corridor: Vector[Point], toleranceM: Double, minAlongFraction: Double = 0.55): Double =
    coTravelChunks(
      route,
      corridor,
      toleranceM,
      minAlongFraction
    ).map(
      _.meters
    ).sum

  /**
   * Pick a Valhalla avoid_location from the actual route that violated a
   * strict /avoid corridor.
   *
   * No junction topology is inferred from raw coordinate equality and there
   * is no fixed endpoint-clearance radius. Among the pieces already accepted
   * by the hard co-travel detector, choose the one deepest inside this
   * connector: maximum distance from its nearer connector endpoint.
   */
  def coTravelBlockPoint(
      route: Vector[Point],
      corridor: Vector[Point],
      toleranceM: Double,
      from: Point,
      to: Point,
      minAlongFraction: Double = 0.55
  ): Option[Point] =
    val chunks = coTravelChunks(route, corridor, toleranceM, minAlongFraction)
    chunks
      .maxByOption { chunk =>
        math.min(
          distanceMeters(
            chunk.midpoint,
            from
          ),
          distanceMeters(chunk.midpoint, to)
        )
      }
      .map(_.midpoint)

  def appendExactDistinctPoints(existing: Vector[Point], additions: Vector[Point]): Vector[Point] =
    additions.foldLeft(
      existing
    ) { (acc, p) =>
      if acc.exists(q => q.lat == p.lat && q.lon == p.lon) then acc
      else acc :+ p
    }

  def appendDistinct(a: Vector[Point], b: Vector[Point], thresholdM: Double = 3.0): Vector[Point] =
    if a.isEmpty then b
    else if b.isEmpty then a
    else if distanceMeters(a.last, b.head) <= thresholdM then a ++ b.tail
    else a ++ b

object Format:
  def duration(seconds: Double): String =
    val total = math.max(0L, math.round(seconds))
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    if h > 0 then f"$h%d:${m}%02d:${s}%02d"
    else f"$m%d:${s}%02d"

  def point(p: Point): String =
    f"${p.lat}%.6f,${p.lon}%.6f"

  def finite(x: Double): Boolean =
    !x.isNaN && !x.isInfinite

class Valhalla(baseUrl: String, profile: RoutingProfile, elevationSampleM: Double):
  private val http = HttpClient.newBuilder().build()
  private val heightShapeMemory =
    mutable.HashMap.empty[
      Vector[(Long, Long)],
      Vector[Point]
    ]

  private def coordinateBits(point: Point): (Long, Long) =
    (
      java.lang.Double.doubleToLongBits(
        point.lat
      ),
      java.lang.Double.doubleToLongBits(point.lon)
    )

  def statusFingerprint(): String =
    try
      val body = get("/status")
      val json = ujson.read(body)
      val obj = json.obj
      val version = obj.get("version").map(_.str).getOrElse("unknown")
      val tiles = obj.get("tileset_last_modified") match
          case Some(ujson.Num(x)) => x.toLong.toString
          case Some(v)            => v.toString
          case None               => "unknown"
      s"valhalla=$version;tiles=$tiles"
    catch
      case _: Exception => "valhalla=unknown;tiles=unknown"

  def route(
      from: Point,
      to: Point,
      avoidLocations: Vector[Point] = Vector.empty,
      speedOverrideKph: Option[Double] = None,
      useHillsOverride: Option[Double] = None,
      useRoadsOverride: Option[Double] = None
  ): Either[String, RouteResult] =
    try
      val payload = ujson.Obj(
        "locations" -> ujson.Arr(
          ujson.Obj("lat" -> from.lat, "lon" -> from.lon),
          ujson.Obj("lat" -> to.lat, "lon" -> to.lon)
        ),
        "costing" -> "bicycle",
        "costing_options" -> ujson.Obj(
          "bicycle" -> ujson.Obj(
            "bicycle_type" -> canonicalBikeType(profile.bicycleType),
            "cycling_speed" -> speedOverrideKph.getOrElse(profile.speedKph),
            "use_hills" -> useHillsOverride.getOrElse(profile.useHills),
            "use_roads" -> useRoadsOverride.getOrElse(profile.useRoads),
            "avoid_bad_surfaces" -> profile.avoidBadSurfaces
          )
        ),
        "directions_type" -> "none",
        "units" -> "kilometers",
        "shape_format" -> "polyline6"
      )
      if avoidLocations.nonEmpty then
        payload.obj("avoid_locations") = ujson.Arr(
          avoidLocations.map { p =>
            ujson.Obj("lat" -> p.lat, "lon" -> p.lon)
          }*
        )
      val body = post("/route", ujson.write(payload))
      val json = ujson.read(body)
      val trip = json.obj.getOrElse("trip", sys.error(s"Valhalla response has no trip: $body"))
      val summary = trip("summary")
      val seconds = summary("time").num
      val lengthKm = summary("length").num
      val legs = trip("legs").arr
      val routePoints = legs.foldLeft(Vector.empty[Point]) { (acc, leg) =>
        val decoded = decodePolyline6(leg("shape").str)
        Geometry.appendDistinct(acc, decoded)
      }
      require(routePoints.nonEmpty, "Valhalla returned an empty route shape")
      val dense = Geometry.resample(routePoints, elevationSampleM)
      val elevated = withElevation(dense)
      Right(
        RouteResult(
          from = from,
          to = to,
          points = elevated,
          rawValhallaPoints = routePoints,
          seconds = seconds,
          lengthKm = lengthKm
        )
      )
    catch
      case e: Exception =>
        val diagnostics =
          try
            val a = locateSummary(from)
            val b = locateSummary(to)
            s"\n  from ${Format.point(from)}: $a\n  to   ${Format.point(to)}: $b"
          catch
            case _: Exception => ""
        Left(Option(e.getMessage).getOrElse(e.toString) + diagnostics)

  private def parseTraceEdges(json: ujson.Value): Vector[TraceEdge] =
    val edges = json.obj
        .get("edges")
        .map(_.arr.toVector)
        .getOrElse(Vector.empty)
    edges.map { edge =>
      val obj = edge.obj

      def string(name: String, default: String = "unknown"): String =
        obj.get(name) match
          case Some(ujson.Str(x)) => x
          case Some(v)            => v.toString
          case None               => default

      def number(name: String): Option[Double] =
        obj.get(name) match
          case Some(ujson.Num(x)) => Some(x)
          case _                  => None

      def bool(name: String): Boolean =
        obj.get(name) match
          case Some(ujson.Bool(x)) => x
          case _                   => false
      TraceEdge(
        lengthKm = number("length").getOrElse(0.0),
        surface = string("surface"),
        use = string("use"),
        roadClass = string("road_class"),
        unpaved = bool("unpaved"),
        cycleLane = string("cycle_lane", "none"),
        beginShapeIndex = number("begin_shape_index")
            .map(
              _.toInt
            ),
        endShapeIndex = number("end_shape_index")
            .map(_.toInt)
      )
    }

  private def traceAttributesRequest(
      points: Vector[Point],
      shapeMatch: String,
      speedKph: Double = 20.0,
      useHills: Double = 0.70,
      useRoads: Double = 0.35
  ): TraceAttributesDetailed =
    val payload = ujson.Obj(
      "shape" -> ujson.Arr(
        points.map(p => ujson.Obj("lat" -> p.lat, "lon" -> p.lon))*
      ),
      "costing" -> "bicycle",
      "costing_options" -> ujson.Obj(
        "bicycle" -> ujson.Obj(
          "bicycle_type" -> canonicalBikeType(profile.bicycleType),
          // edge_walk must use the same routing preferences that produced the
          // Valhalla route. This avoids rejecting a valid exact walk merely
          // because the trace request used a different candidate profile.
          "cycling_speed" -> speedKph,
          "use_hills" -> useHills,
          "use_roads" -> useRoads,
          "avoid_bad_surfaces" -> profile.avoidBadSurfaces
        )
      ),
      "shape_match" -> shapeMatch,
      "shape_format" -> "polyline6",
      "directions_options" -> ujson.Obj(
        "units" -> "kilometers"
      ),
      "filters" -> ujson.Obj(
        "action" -> "include",
        "attributes" -> ujson.Arr(
          "edge.length",
          "edge.begin_shape_index",
          "edge.end_shape_index",
          "edge.road_class",
          "edge.use",
          "edge.unpaved",
          "edge.surface",
          "edge.cycle_lane",
          "shape"
        )
      )
    )
    val body = post("/trace_attributes", ujson.write(payload))
    val json = ujson.read(body)
    val edges = parseTraceEdges(json)
    val matchedShape = json.obj
        .get("shape")
        .collect {
          case ujson.Str(encoded)
              if encoded.nonEmpty =>
            decodePolyline6(encoded)
        }
        .getOrElse(Vector.empty)
    TraceAttributesDetailed(edges, matchedShape)

  private def elevateTraceForSafety(trace: TraceAttributesDetailed): Either[String, TraceAttributesDetailed] =
    val elevatedShape = withElevation(trace.shape)
    if elevatedShape.size == trace.shape.size &&
        elevatedShape.forall(_.ele.isDefined)
    then
      Right(trace.copy(shape = elevatedShape))
    else
      Left(
        "trace_attributes matched shape has incomplete elevation for safety classification"
      )

  def traceAttributesDetailed(
      points: Vector[Point],
      speedKph: Double = 20.0,
      useHills: Double = 0.70,
      useRoads: Double = 0.35
  ): Either[String, TraceAttributesDetailed] =
    try
      require(points.size >= 2, "Need at least two points for trace_attributes")
      val exact = traceAttributesRequest(points, "edge_walk", speedKph, useHills, useRoads)
      val usable = exact.edges.nonEmpty &&
          exact.shape.size >= 2 &&
          exact.edges.forall(
            edge =>
              edge.beginShapeIndex.nonEmpty &&
                edge.endShapeIndex.nonEmpty
          )
      if usable then elevateTraceForSafety(exact)
      else Left("trace_attributes edge_walk returned no shape-indexed edge geometry")
    catch
      case e: Exception =>
        Left(
          Option(
            e.getMessage
          ).getOrElse(e.toString)
        )

  def withElevation(points: Vector[Point]): Vector[Point] =
    val cacheKey = points.map(coordinateBits)
    heightShapeMemory.get(
      cacheKey
    ) match
      case Some(elevated) =>
        elevated
      case None =>
        val elevated = points.grouped(500).flatMap { chunk =>
            val payload = ujson.Obj(
              "shape" -> ujson.Arr(chunk.map { p =>
                ujson.Obj("lat" -> p.lat, "lon" -> p.lon)
              }*)
            )
            val body = post("/height", ujson.write(payload))
            val json = ujson.read(body)
            val heights = json("height").arr.toVector.map {
              case ujson.Num(x) => Some(x)
              case ujson.Null   => None
              case _            => None
            }
            require(
              heights.size == chunk.size,
              s"Valhalla /height returned ${heights.size} heights for ${chunk.size} points"
            )
            chunk.zip(heights).map { case (p, ele) =>
              p.copy(ele = ele)
            }
          }.toVector
        heightShapeMemory.update(cacheKey, elevated)
        elevated

  private def locateSummary(point: Point): String =
    val payload = ujson.Obj(
      "verbose" -> true,
      "locations" -> ujson.Arr(
        ujson.Obj("lat" -> point.lat, "lon" -> point.lon)
      ),
      "costing" -> "bicycle"
    )
    val body = post("/locate", ujson.write(payload))
    val json = ujson.read(body)
    val first = json.arr.head
    val edges = first("edges").arr
    if edges.isEmpty then "no bicycle edges"
    else
      val nearest = edges.map { edge =>
        edge.obj.get("distance").map(_.num).getOrElse(Double.PositiveInfinity)
      }.min
      f"${edges.size}%d bicycle edge(s), nearest ${nearest}%.1f m"

  private def get(path: String): String =
    val req = HttpRequest
      .newBuilder(URI.create(baseUrl + path))
      .header("Accept", "application/json")
      .GET()
      .build()
    val res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
    if res.statusCode() / 100 != 2 then
      sys.error(s"Valhalla ${res.statusCode()} for $path:\n${res.body()}")
    res.body()

  private def post(path: String, json: String): String =
    val req = HttpRequest
      .newBuilder(URI.create(baseUrl + path))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
      .build()
    val res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
    if res.statusCode() / 100 != 2 then
      sys.error(s"Valhalla ${res.statusCode()} for $path:\n${res.body()}")
    res.body()

  private def canonicalBikeType(s: String): String =
    s.toLowerCase match
      case "road"     => "Road"
      case "hybrid"   => "Hybrid"
      case "city"     => "City"
      case "cross"    => "Cross"
      case "mountain" => "Mountain"
      case other       => sys.error(s"Unsupported bicycle type: $other")

  private def decodePolyline6(encoded: String): Vector[Point] =
    val out = Vector.newBuilder[Point]
    var index = 0
    var lat = 0L
    var lon = 0L

    def nextDelta(): Long =
      var result = 0L
      var shift = 0
      var keepReading = true
      while keepReading do
        if index >= encoded.length then
          sys.error("Invalid/truncated Valhalla polyline")
        val value = encoded.charAt(index).toInt - 63
        index += 1
        result |= ((value & 0x1f).toLong << shift)
        shift += 5
        keepReading = value >= 0x20
      if (result & 1L) == 0L then result >> 1
      else ~(result >> 1)
    while index < encoded.length do
      lat += nextDelta()
      lon += nextDelta()
      out += Point(lat / 1e6, lon / 1e6)
    out.result()

object RouteSearchProfiles:
  // Minimal simultaneous profile cover validated against the canonical product
  // graph and rider outputs. Recompute only when the canonical dataset or
  // connector/safety semantics materially change.
  val all: Vector[SearchProfileKey] =
    Vector(
      SearchProfileKey(20.0, 0.25, 0.35),
      SearchProfileKey(20.0, 0.50, 0.35),
      SearchProfileKey(20.0, 0.90, 0.35),
      SearchProfileKey(15.0, 0.05, 0.35),
      SearchProfileKey(15.0, 0.50, 0.75),
      SearchProfileKey(15.0, 0.90, 0.35),
      SearchProfileKey(15.0, 0.90, 0.75),
      SearchProfileKey(25.0, 0.05, 0.35),
      SearchProfileKey(25.0, 0.90, 0.35),
      SearchProfileKey(15.0, 0.70, 0.55),
      SearchProfileKey(25.0, 0.16, 0.75),
      SearchProfileKey(20.0, 0.98, 0.55)
    )

  val provenance: String =
    s"${all.size} production profiles = validated minimal simultaneous cover for the current canonical dataset"

class RouteCache:
  // Run-local cache only. The planner no longer writes mutable routing cache
  // files to the user's filesystem. This still de-duplicates repeated Valhalla
  // calls between the two candidate-endpoint matrix builds in one invocation.
  private case class Key(
      fromLatBits: Long,
      fromLonBits: Long,
      toLatBits: Long,
      toLonBits: Long,
      avoidBits: Vector[(Long, Long)],
      speedBits: Long,
      hillsBits: Long,
      roadsBits: Long
  )
  private val memory =
    mutable.HashMap.empty[
      Key,
      RouteResult
    ]

  // Exact run-local cache for /trace_attributes. A successful /route request is
  // often reused for another mandatory GPX with identical connector endpoints.
  // Previously the route itself was cached but its exact edge_walk trace was
  // recomputed every time. The trace request is a pure function of the same
  // route-cache key and returned raw shape, so reusing it changes no connector
  // semantics and creates no persistent filesystem state.
  private val traceMemory =
    mutable.HashMap.empty[
      Key,
      TraceAttributesDetailed
    ]

  case class Fetch(route: RouteResult, cacheHit: Boolean)

  private def bits(value: Double): Long =
    java.lang.Double.doubleToLongBits(value)

  private def key(
      from: Point,
      to: Point,
      avoidLocations: Vector[Point],
      speedOverrideKph: Option[Double],
      useHillsOverride: Option[Double],
      useRoadsOverride: Option[Double]
  ): Key =
    Key(
      fromLatBits = bits(from.lat),
      fromLonBits = bits(from.lon),
      toLatBits = bits(to.lat),
      toLonBits = bits(to.lon),
      avoidBits =
        avoidLocations.map(
          point =>
            (bits(point.lat), bits(point.lon))
        ),
      speedBits =
        bits(
          speedOverrideKph.getOrElse(Double.NaN)
        ),
      hillsBits =
        bits(
          useHillsOverride.getOrElse(Double.NaN)
        ),
      roadsBits = bits(useRoadsOverride.getOrElse(Double.NaN))
    )

  def getOrRoute(
      from: Point,
      to: Point,
      valhalla: Valhalla,
      avoidLocations: Vector[Point] = Vector.empty,
      speedOverrideKph: Option[Double] = None,
      useHillsOverride: Option[Double] = None,
      useRoadsOverride: Option[Double] = None
  ): Either[String, Fetch] =
    val cacheKey = key(from, to, avoidLocations, speedOverrideKph, useHillsOverride, useRoadsOverride)
    memory.get(
      cacheKey
    ) match
      case Some(route) =>
        Right(Fetch(route, cacheHit = true))
      case None =>
        valhalla.route(
          from,
          to,
          avoidLocations,
          speedOverrideKph,
          useHillsOverride,
          useRoadsOverride
        ).map { route =>
          memory.update(cacheKey, route)
          Fetch(route, cacheHit = false)
        }

  def getOrTraceAttributes(
      from: Point,
      to: Point,
      route: RouteResult,
      valhalla: Valhalla,
      avoidLocations: Vector[Point],
      speedKph: Double,
      useHills: Double,
      useRoads: Double
  ): Either[String, TraceAttributesDetailed] =
    // The exact edge_walk must use the *resolved* routing preferences that
    // produced this route. Keep them explicit here instead of inventing
    // fallback defaults inside the cache: that makes the trace-cache key and
    // the Valhalla trace request describe exactly the same route/profile.
    val cacheKey = key(from, to, avoidLocations, Some(speedKph), Some(useHills), Some(useRoads))
    traceMemory.get(cacheKey) match
      case Some(trace) =>
        Right(trace)
      case None =>
        valhalla.traceAttributesDetailed(
          route.rawValhallaPoints,
          speedKph,
          useHills,
          useRoads
        ).map { trace =>
          traceMemory.update(cacheKey, trace)
          trace
        }

object TransferModel:
  // Hardcoded MTB transfer assumptions. These are not user knobs.
  // Surface affects rolling resistance. "path" is allowed, but mildly penalized
  // so a normal track/road transfer wins when it is reasonably competitive.
  val DefaultCrr = 0.010
  val PathPenaltySecondsPerKm = 150.0

  def surfaceCrr(surface: String, unpaved: Boolean): Double =
    surface.toLowerCase match
      case "paved_smooth" => 0.006
      case "paved"        => 0.007
      case "compacted"    => 0.010
      case "gravel"       => 0.013
      case "dirt"         => 0.016
      case "mud"          => 0.025
      case "sand"         => 0.030
      case "impassable"   => 0.050
      case _               => if unpaved then 0.014 else DefaultCrr

  def effectiveCrr(edges: Vector[TraceEdge]): Double =
    val totalKm = edges.map(_.lengthKm).sum
    if totalKm <= 1e-9 then DefaultCrr
    else
      edges.map { e =>
        e.lengthKm * surfaceCrr(e.surface, e.unpaved)
      }.sum / totalKm

  def pathFraction(edges: Vector[TraceEdge]): Double =
    val totalKm = edges.map(_.lengthKm).sum
    if totalKm <= 1e-9 then 0.0
    else
      edges.filter(_.use == "path").map(_.lengthKm).sum / totalKm

  def qualityPenaltySeconds(edges: Vector[TraceEdge]): Double =
    val pathKm = edges.filter(_.use == "path").map(_.lengthKm).sum
    pathKm * PathPenaltySecondsPerKm


object Scoring:
  def connector(
      route: RouteResult,
      cfg: Config,
      trace: TraceAttributesDetailed,
      routingSpeedKph: Double,
      routingUseHills: Double,
      routingUseRoads: Double
  ): Connector =
    val traceEdges = trace.edges
    val (ascent, descent) = Geometry.ascentDescent(route.points)
    val lateAscent = Geometry.exponentiallyWeightedAscentNearEnd(route.points, cfg.arrivalWindowM)
    val crr = TransferModel.effectiveCrr(traceEdges)
    val pathFraction = TransferModel.pathFraction(traceEdges)
    val physics = RidePhysics.estimate(route.points, cfg, downhillCapKph = None, crrOverride = crr)

    // Climbing is already priced by rider power/time. Keep only:
    // 1) arrival-recovery preference and
    // 2) a small preference for transfer-like track/road over OSM "path".
    val arrivalPenaltySeconds = lateAscent / 100.0 * cfg.arrivalClimbPenaltyMinPer100m * 60.0
    val qualityPenaltySeconds = TransferModel.qualityPenaltySeconds(traceEdges)
    val candidateComfortPenaltySeconds =
      PowerPolicy.candidateComfortThresholdsW(cfg.riderPowerW)
        .map(physics.abovePowerSeconds)
        .sum

    // ---------------------------------------------------------------
    // ROAD + TECHNICAL-DOWNHILL EDGE ALIGNMENT
    //
    // Hard road/downhill safety must use the actual shape-indexed Valhalla
    // edge geometry. The old implementation allocated whole-route physics
    // time by edge-length fraction and classified a 30/100 m grade window by
    // whichever edge contained its midpoint. Both are unsafe at boundaries.
    //
    // Valhalla begin/end_shape_index refer to the returned trace shape.
    // Carry the already-elevated route profile onto that matched shape by
    // horizontal projection, then evaluate contiguous edge runs directly.
    // -----------------------------------------------------------------

    // traceAttributesDetailed already returns the exact edge-walk matched shape
    // with its own Valhalla elevation. Do not re-project it onto route.points:
    // nearest-point projection is ambiguous at self-intersections/overpasses and
    // can transfer elevation from the wrong traversal.
    val traceShapeElevated = trace.shape

    def validEdgeRange(edge: TraceEdge): Option[(Int, Int)] =
      for
        begin <-
          edge.beginShapeIndex
        end <-
          edge.endShapeIndex
        if begin >= 0
        if end >= begin
        if end < traceShapeElevated.size
      yield
        (begin, end)
    require(
      traceShapeElevated.size >= 2 &&
        traceEdges.nonEmpty &&
        traceEdges.forall(
          edge =>
            validEdgeRange(
              edge
            ).nonEmpty
        ),
      "Shape-indexed trace geometry is required for connector safety classification"
    )

    case class EdgeRun(beginShapeIndex: Int, endShapeIndex: Int, edges: Vector[TraceEdge])

    def edgeRuns(
        predicate: TraceEdge => Boolean
    ): Vector[EdgeRun] =
      val out = Vector.newBuilder[EdgeRun]
      var activeBegin = -1
      var activeEnd = -1
      val activeEdges = mutable.ArrayBuffer.empty[TraceEdge]

      def flush(): Unit =
        if activeBegin >= 0 &&
            activeEnd >= activeBegin &&
            activeEdges.nonEmpty
        then
          out +=
            EdgeRun(activeBegin, activeEnd, activeEdges.toVector)
        activeBegin = -1
        activeEnd = -1
        activeEdges.clear()
      traceEdges.foreach { edge =>
        val (
          begin,
          end
        ) =
          validEdgeRange(
            edge
          ).get
        if predicate(edge)
        then
          if activeBegin < 0 then
            activeBegin = begin
            activeEnd = end
          else if begin <= activeEnd + 1
          then
            activeEnd = math.max(activeEnd, end)
          else
            flush()
            activeBegin = begin
            activeEnd = end
          activeEdges +=
            edge
        else
          flush()
      }
      flush()
      out.result()

    def runPoints(run: EdgeRun): Vector[Point] =
      traceShapeElevated.slice(run.beginShapeIndex, run.endShapeIndex + 1)

    def modeledRunSeconds(run: EdgeRun): Double =
      RidePhysics.estimate(
        runPoints(
          run
        ),
        cfg,
        downhillCapKph = None,
        crrOverride = TransferModel.effectiveCrr(run.edges)
      ).totalSeconds

    def modeledEdgeSeconds(
        predicate: TraceEdge => Boolean
    ): Double =
      edgeRuns(
        predicate
      ).map(
        modeledRunSeconds
      ).sum

    def cycleLaneKind(edge: TraceEdge): String =
      edge.cycleLane.trim.toLowerCase match
        case "" | "no" | "unknown" | "false" =>
          "none"
        case other =>
          other

    val hasMotorwayTrunk =
      traceEdges.exists(
        edge =>
          Set(
            "motorway",
            "trunk"
          )(edge.roadClass)
      )
    val majorRoadSeconds = modeledEdgeSeconds { edge =>
        Set("motorway", "trunk", "primary")(edge.roadClass)
      }
    val motorwayTrunkSeconds = modeledEdgeSeconds { edge =>
        Set("motorway", "trunk")(edge.roadClass)
      }
    val primaryNoCycleSeconds = modeledEdgeSeconds { edge =>
        edge.roadClass == "primary" &&
          cycleLaneKind(edge) == "none"
      }
    val primarySharedSeconds = modeledEdgeSeconds { edge =>
        edge.roadClass == "primary" &&
          cycleLaneKind(edge) == "shared"
      }
    val primaryWithCycleSeconds = modeledEdgeSeconds { edge =>
        edge.roadClass == "primary" &&
          cycleLaneKind(edge) != "none"
      }
    val secondaryNoCycleSeconds = modeledEdgeSeconds { edge =>
        edge.roadClass == "secondary" &&
          cycleLaneKind(edge) == "none"
      }
    val secondarySharedSeconds = modeledEdgeSeconds { edge =>
        edge.roadClass == "secondary" &&
          cycleLaneKind(edge) == "shared"
      }
    val longestLowProtectionPrimarySeconds = edgeRuns { edge =>
        edge.roadClass == "primary" &&
          Set(
            "none",
            "shared"
          )(cycleLaneKind(edge))
      }.map(
        modeledRunSeconds
      ).foldLeft(
        0.0
      )(math.max)
    val unpavedSeconds = modeledEdgeSeconds(_.unpaved)

    def technicalDownhillEdge(edge: TraceEdge): Boolean =
      edge.unpaved ||
        (
          edge.use == "path" &&
            !Set("paved_smooth", "paved", "asphalt", "concrete")(edge.surface.trim.toLowerCase)
        )

    def technicalPathEdge(edge: TraceEdge): Boolean =
      edge.use == "path" && technicalDownhillEdge(edge)

    // Hard downhill metrics are evaluated only inside contiguous technical
    // runs. A paved midpoint can no longer hide an otherwise technical 30 m
    // window, and a one-metre technical edge cannot contaminate a mostly paved
    // 30 m window.
    def maxRunDescentPct(
        predicate: TraceEdge => Boolean,
        windowM: Double
    ): Double =
      edgeRuns(
        predicate
      ).map(
        run =>
          Geometry.maxSustainedDescentPct(
            runPoints(
              run
            ),
            windowM
          )
      ).foldLeft(
        0.0
      )(math.max)
    val maxTechnicalDownhillGrade30Pct = maxRunDescentPct(technicalDownhillEdge, 30.0)
    val maxTechnicalDownhillGrade100Pct = maxRunDescentPct(technicalDownhillEdge, 100.0)
    val maxTechnicalPathDownhillGrade30Pct = maxRunDescentPct(technicalPathEdge, 30.0)

    // Moderate handling is a soft ranking metric, but its geometry must not
    // depend on an arbitrary 10 m phase. Compute the union of actual ridden
    // distance covered by qualifying 30 m windows inside category-pure runs.
    case class HandlingCoverage(startM: Double, endM: Double, seconds: Double)
    val traceCumulative = Geometry.pointsWithCumulativeDistance(traceShapeElevated)

    def shapeDistanceAtIndex(index: Int): Double =
      traceCumulative(
        math.max(0, math.min(traceCumulative.size - 1, index))
      )._1

    def handlingCoverageForRuns(
        predicate: TraceEdge => Boolean,
        thresholdPct: Double,
        referenceSpeedKph: Double
    ): Vector[HandlingCoverage] =
      edgeRuns(
        predicate
      ).flatMap { run =>
        val points = runPoints(run)
        val runStartM = shapeDistanceAtIndex(run.beginShapeIndex)
        Geometry.sustainedDescentCoverageIntervals(
          points,
          windowM = 30.0,
          minDescentPct = thresholdPct
        ).map {
          case (
                localStartM,
                localEndM
              ) =>
            val distanceM =
              math.max(
                0.0,
                localEndM -
                  localStartM
              )
            HandlingCoverage(
              startM =
                runStartM +
                  localStartM,
              endM =
                runStartM +
                  localEndM,
              seconds =
                distanceM /
                  (
                    referenceSpeedKph /
                      3.6
                  )
            )
        }
      }
    val pathHandlingCoverage = handlingCoverageForRuns(technicalPathEdge, thresholdPct = 10.0, referenceSpeedKph = 6.0)
    val roughNonPathHandlingCoverage =
      handlingCoverageForRuns(
        edge =>
          technicalDownhillEdge(
            edge
          ) &&
            !technicalPathEdge(
              edge
            ),
        thresholdPct = 14.0,
        referenceSpeedKph = 8.0
      )
    val handlingCoverage =
      (
        pathHandlingCoverage ++
          roughNonPathHandlingCoverage
      ).sortBy(_.startM)
    val downhillHandlingSeconds =
      handlingCoverage.map(
        _.seconds
      ).sum
    val mergedHandlingCoverage =
      handlingCoverage.foldLeft(
        Vector.empty[(Double, Double)]
      ) {
        case (
              acc,
              coverage
            ) =>
          acc.lastOption match
            case Some((previousStartM, previousEndM))
                if coverage.startM <= previousEndM + 1e-6 =>
              acc.dropRight(
                1
              ) :+
                (previousStartM, math.max(previousEndM, coverage.endM))
            case _ =>
              acc :+
                (coverage.startM, coverage.endM)
      }
    val longestTechnicalDownhillRunM =
      mergedHandlingCoverage.map {
        case (
              startM,
              endM
            ) =>
          math.max(
            0.0,
            endM -
              startM
          )
      }.foldLeft(
        0.0
      )(math.max)
    val connectorProfile = Geometry.profileStats(route.points)
    val maxGrade30Pct = connectorProfile.maxGrade30Pct
    val maxGrade100Pct = connectorProfile.maxGrade100Pct
    Connector(
      route = route,
      ascentM = ascent,
      descentM = descent,
      lateAscentM = lateAscent,
      physicsSeconds = physics.totalSeconds,
      fatiguePenaltySeconds = arrivalPenaltySeconds,
      transferQualityPenaltySeconds = qualityPenaltySeconds,
      candidateComfortPenaltySeconds = candidateComfortPenaltySeconds,
      maxRiderPowerW = physics.maxRiderPowerW,
      maxGrade30Pct = maxGrade30Pct,
      maxGrade100Pct = maxGrade100Pct,
      majorRoadSeconds = majorRoadSeconds,
      motorwayTrunkSeconds = motorwayTrunkSeconds,
      hasMotorwayTrunk = hasMotorwayTrunk,
      primaryNoCycleSeconds = primaryNoCycleSeconds,
      primarySharedSeconds = primarySharedSeconds,
      primaryWithCycleSeconds = primaryWithCycleSeconds,
      secondaryNoCycleSeconds = secondaryNoCycleSeconds,
      secondarySharedSeconds = secondarySharedSeconds,
      longestLowProtectionPrimarySeconds = longestLowProtectionPrimarySeconds,
      unpavedSeconds = unpavedSeconds,
      downhillHandlingSeconds = downhillHandlingSeconds,
      longestTechnicalDownhillRunM = longestTechnicalDownhillRunM,
      maxTechnicalDownhillGrade30Pct = maxTechnicalDownhillGrade30Pct,
      maxTechnicalDownhillGrade100Pct = maxTechnicalDownhillGrade100Pct,
      maxTechnicalPathDownhillGrade30Pct = maxTechnicalPathDownhillGrade30Pct,
      effectiveCrr = crr,
      pathFraction = pathFraction,
      routingSpeedKph = routingSpeedKph,
      routingUseHills = routingUseHills,
      routingUseRoads = routingUseRoads,
      traceEdges = traceEdges,
      powerAboveSecondsByThreshold = physics.powerAboveSeconds,
      longestPowerStreakSecondsByThreshold = physics.longestPowerStreakSeconds,
      candidateComfortSpikeLoadSeconds = physics.candidateComfortSpikeLoadSeconds
    )

case class Matrix(
    startToTrail: Vector[Option[Connector]],
    between: Vector[Vector[Option[Connector]]],
    trailToFinish: Vector[Option[Connector]],
    startToFinish: Option[Connector]
)

object DifferentialEvidence:
  private val rerouteLines = mutable.ArrayBuffer.empty[String]
  def add(line: String): Unit = rerouteLines.synchronized { rerouteLines += line }
  def snapshot: Vector[String] = rerouteLines.synchronized { rerouteLines.toVector }

object CorridorPolicy:
  // Canonical GPX endpoints are the exact connector join boundaries.
  // `corridorToleranceM` is GPS/GPX matching tolerance, NOT permission to ride
  // that many metres of a protected trail. Every mandatory and explicit
  // /avoid GPX is protected over its full supplied geometry.
  val corridorToleranceM = 12.0
  val maxConnectorCoTravelM = 12.0

object MatrixBuilder:
  def build(
      start: Point,
      trails: Vector[Trail],
      finish: Trail,
      router: RouteCache,
      valhalla: Valhalla,
      cfg: Config,
      safetyReservedTrails: Vector[Trail],
      strictForbiddenTrails: Vector[Trail],
      buildBetween: Boolean = true
  ): Matrix =
    val n = trails.size
    val OverlapToleranceM = CorridorPolicy.corridorToleranceM
    val reservedTrails = safetyReservedTrails.distinctBy(_.path)
    val overlapSummaryMemory = mutable.HashMap.empty[
        Vector[Point],
        Vector[(Trail, Double)]
      ]

    case class ScoringSemanticKey(points: Vector[Point], trace: TraceAttributesDetailed)
    val scoringMemory = mutable.HashMap.empty[
        ScoringSemanticKey,
        Connector
      ]

    def overlapSummary(route: RouteResult): Vector[(Trail, Double)] =
      overlapSummaryMemory.get(
        route.points
      ) match
        case Some(overlaps) =>
          overlaps
        case None =>
          val overlaps = reservedTrails
              .map { trail =>
                trail ->
                  Geometry.coTravelOverlapMeters(route.points, trail.points, OverlapToleranceM)
              }
              .filter { case (_, meters) =>
                meters >
                  CorridorPolicy.maxConnectorCoTravelM
              }
              .sortBy { case (_, meters) =>
                -meters
              }
          overlapSummaryMemory.update(route.points, overlaps)
          overlaps

    def blockPoint(badTrail: Trail, violatingRoute: RouteResult, from: Point, to: Point): Option[Point] =
      Geometry.coTravelBlockPoint(violatingRoute.points, badTrail.points, OverlapToleranceM, from, to)

    val SearchProfiles = RouteSearchProfiles.all

    // Every protected-corridor blocker is genuinely monotone: once a
    // blocker point has been sent to Valhalla it is retained for every later
    // reroute. Repeated violation of the same GPX may add another blocker.
    //
    // The cap is only an operational fail-closed guard, not a completeness
    // theorem. Normal termination is driven by actual new constraint points.
    val MaxSafetyReroutes =
      math.max(
        64,
        reservedTrails.map(t => math.max(1, t.points.size - 1)).sum
      )

    case class SafeCandidate(connector: Connector)

    def safeCandidate(label: String, from: Point, to: Point, speedKph: Double, useHills: Double, useRoads: Double): Either[String, SafeCandidate] =
      var avoidLocations = Vector.empty[Point]
      var reroutes = 0
      var lastProblem = ""
      while reroutes <= MaxSafetyReroutes do
        router.getOrRoute(
          from,
          to,
          valhalla,
          avoidLocations = avoidLocations,
          speedOverrideKph = Some(speedKph),
          useHillsOverride = Some(useHills),
          useRoadsOverride = Some(useRoads)
        ) match
          case Left(error) =>
            return Left(error)
          case Right(fetch) =>
            val badOverlaps = overlapSummary(fetch.route)
            if badOverlaps.isEmpty then
              val trace =
                router.getOrTraceAttributes(
                  from,
                  to,
                  fetch.route,
                  valhalla,
                  avoidLocations = avoidLocations,
                  speedKph = speedKph,
                  useHills = useHills,
                  useRoads = useRoads
                ) match
                  case Right(trace) =>
                    trace
                  case Left(error) =>
                    return Left(s"exact edge_walk unavailable for safety classification: $error")
              val scoringKey = ScoringSemanticKey(points = fetch.route.points, trace = trace)
              val connector =
                scoringMemory.get(
                  scoringKey
                ) match
                  case Some(base) =>
                    base.copy(
                      route = fetch.route,
                      routingSpeedKph = speedKph,
                      routingUseHills = useHills,
                      routingUseRoads = useRoads
                    )
                  case None =>
                    val computed =
                      Scoring.connector(
                        fetch.route,
                        cfg,
                        trace,
                        routingSpeedKph = speedKph,
                        routingUseHills = useHills,
                        routingUseRoads = useRoads
                      )
                    scoringMemory.update(scoringKey, computed)
                    computed
              return Right(SafeCandidate(connector))
            lastProblem = badOverlaps
                .map { case (trail, meters) =>
                  f"${trail.name}=${meters}%.0f m"
                }
                .mkString(", ")
            val derivedBlockersByTrail = badOverlaps.map { case (trail, overlapM) =>
                val blocker = blockPoint(
                  trail,
                  fetch.route,
                  from,
                  to
                )
                (trail, overlapM, blocker)
              }
            val derivedBlockers = derivedBlockersByTrail.flatMap(_._3.toVector)

            if label == "Bunker -> RegenbogenAbzweiger" then
              val routeDistanceM = fetch.route.lengthKm * 1000.0
              derivedBlockersByTrail.foreach { case (trail, overlapM, blocker) =>
                val blockerText = blocker match
                  case None => "NONE"
                  case Some(p) =>
                    val depth = math.min(
                      Geometry.distanceMeters(p, from),
                      Geometry.distanceMeters(p, to)
                    )
                    f"${p.lat}%.6f,${p.lon}%.6f depth=$depth%.1fm"
                DifferentialEvidence.add(
                  f"old-blocker label=$label profile=v$speedKph%.0f-h$useHills%.2f-r$useRoads%.2f reroute=$reroutes " +
                    f"routeDistance=$routeDistanceM%.1fm corridor=${trail.name} overlap=$overlapM%.3fm blocker=[$blockerText]"
                )
              }

            if derivedBlockers.size <
                badOverlaps.size
            then
              return Left(
                s"trail-safety detected protected-corridor overlap but could not derive a blocker: $lastProblem"
              )
            val accumulated = Geometry.appendExactDistinctPoints(avoidLocations, derivedBlockers)
            val added = accumulated.size -
                avoidLocations.size
            avoidLocations = accumulated
            if added == 0 then
              return Left(s"trail-safety made no constraint progress on repeated overlap: $lastProblem")
            reroutes +=
              1
      Left(s"trail-safety reached defensive reroute cap $MaxSafetyReroutes; last overlap: $lastProblem")

    def fetch(label: String, from: Point, to: Point): Option[Connector] =
      def directAttempt(profile: SearchProfileKey): Either[String, SafeCandidate] =
        safeCandidate(label, from, to, profile.speedKph, profile.useHills, profile.useRoads)

      val directAttempts = SearchProfiles.map { profile =>
          profile ->
            directAttempt(profile)
        }
      val directResults =
        directAttempts.flatMap {
          case (_, Right(candidate)) =>
            Some(candidate)
          case _ =>
            None
        }
      val results = directResults
      val failureReasons =
        directAttempts.flatMap {
          case (_, Left(error)) =>
            Some("direct: " + error)
          case _ =>
            None
        }
      if results.isEmpty then
        val reasonText =
          if failureReasons.isEmpty then ""
          else
            val grouped = failureReasons
                .groupBy(identity)
                .toVector
                .map { case (reason, xs) => reason -> xs.size }
                .sortBy { case (_, count) => -count }
            val (reason, count) = grouped.head
            s"; most common failure $count/${math.max(1, directAttempts.size)}: $reason"
        Console.err.println(
          s"UNREACHABLE $label" +
            reasonText
        )
        None
      else
        // Keep every successful SearchProfile candidate here.
        // Bit-identical routed geometry is NOT solver-semantic equality:
        // coincident graph edges may carry different road/surface metadata.
        // Exact no-worse dominance is applied later, after all hard-safety
        // dimensions are present.
        val sortedByScore = results.sortBy { x =>
            (
              x.connector.physicsSeconds,
              x.connector.candidateComfortPenaltySeconds,
              x.connector.transferQualityPenaltySeconds,
              x.connector.fatiguePenaltySeconds,
              x.connector.pathFraction
            )
          }
        val rawBestSeconds = sortedByScore.head.connector.physicsSeconds

        // Route-time / DEM / surface physics is not precise to a few seconds.
        // Treat routes within roughly 5% as effectively tied, then prefer the
        // one with lower V5 candidate comfort burden, then path/arrival burden.
        // No fixed legacy power threshold or weighted effort score participates.
        val indifferenceSeconds = math.min(45.0, math.max(10.0, rawBestSeconds * 0.05))
        val nearEqual = sortedByScore.filter { x =>
            x.connector.physicsSeconds <= rawBestSeconds + indifferenceSeconds
          }
        val chosenFetch = nearEqual.minBy { x =>
            (
              x.connector.candidateComfortPenaltySeconds,
              x.connector.transferQualityPenaltySeconds,
              x.connector.fatiguePenaltySeconds,
              x.connector.pathFraction,
              x.connector.physicsSeconds
            )
          }
        val best0 = chosenFetch.connector
        val best =
          best0.copy(searchConnectorVariants = sortedByScore.map(_.connector))
        Some(best)
    if n == 0 then
      val direct = fetch(s"${finish.name} -> ${finish.name}", start, finish.start)
      Matrix(Vector.empty, Vector.empty, Vector.empty, direct)
    else
      val startToTrail = trails.indices.map { i =>
        fetch(s"${finish.name} -> ${trails(i).name}", start, trails(i).start)
      }.toVector
      val between =
        if !buildBetween then Vector.fill(n)(Vector.fill[Option[Connector]](n)(None))
        else trails.indices.map { i =>
            trails.indices.map { j =>
              if i == j then None
              else
                fetch(
                  s"${trails(i).name} -> ${trails(j).name}",
                  trails(i).end,
                  trails(j).start
                )
            }.toVector
          }.toVector
      val trailToFinish = trails.indices.map { i =>
        fetch(
          s"${trails(i).name} -> ${finish.name}",
          trails(i).end,
          finish.start
        )
      }.toVector
      Matrix(startToTrail, between, trailToFinish, None)

case class RideTimeEstimate(
    totalSeconds: Double,
    poweredSeconds: Double,
    coastingSeconds: Double,
    coastingDistanceKm: Double,
    modeledDistanceKm: Double,
    skippedDistanceKm: Double,
    riderEnergyJ: Double,
    maxRiderPowerW: Double,
    powerAboveSeconds: Map[Int, Double],
    leadingNonCoastSeconds: Double = 0.0,
    trailingNonCoastSeconds: Double = 0.0,
    internalNonCoastRunsSeconds: Vector[Double] = Vector.empty,
    longestPowerStreakSeconds: Map[Int, Double] = Map.empty,
    candidateComfortSpikeLoadSeconds: Double = 0.0
):
  def abovePowerSeconds(thresholdW: Int): Double =
    powerAboveSeconds.getOrElse(thresholdW, 0.0)

object RidePhysics:
  private val Gravity = 9.80665
  private val AirDensity = 1.225
  private val CdA = 0.60
  private val DefaultCrr = 0.010
  private val DrivetrainEfficiency = 0.95
  // /height elevation is not reliable enough for instantaneous 10-20 m power spikes.
  // 30 m still captures short climbs, while suppressing single-sample DEM steps.
  private val GradeWindowM = 30.0

  // Fixed bike gearing for this rider/bike.
  // Not exposed as CLI knobs: these are hardware facts, not route preferences.
  private val WheelDiameterIn = 27.5
  private val FrontTeeth = 32.0
  private val RearTeeth = 51.0
  private val PreferredCadenceRpm = 80.0
  private val MinCadenceRpm = 45.0

  def powerThresholdsW(targetW: Double): Vector[Int] =
    PowerPolicy.trackedThresholdsW(targetW)

  // Steep+twisty ordering uses geometry/elevation only; rider speed and old
  // subset/flow rewards are intentionally not part of trail inclusion.
  val NoviceSteepTwistyMinNetDescentPct: Double = 10.0
  val NoviceSteepTwistyMinSinuosity: Double = 1.10

  // Strong local-window detector. Local windows are noisier and common bends
  // are frequent, so thresholds are deliberately stronger than whole-trail.
  val NoviceSteepTwistyLocalShortWindowM: Double = 60.0
  val NoviceSteepTwistyLocalShortMinNetDescentPct: Double = 18.0
  val NoviceSteepTwistyLocalLongWindowM: Double = 100.0
  val NoviceSteepTwistyLocalLongMinNetDescentPct: Double = 15.0
  val NoviceSteepTwistyLocalMinSinuosity: Double = 1.20

  def minPedalingSpeedKph: Double =
    val wheelDiameterM = WheelDiameterIn * 0.0254
    val wheelCircumferenceM = math.Pi * wheelDiameterM
    val wheelRpm = MinCadenceRpm * FrontTeeth / RearTeeth
    wheelRpm * wheelCircumferenceM * 60.0 / 1000.0

  def estimate(
      points: Vector[Point],
      cfg: Config,
      downhillCapKph: Option[Double] = None,
      crrOverride: Double = DefaultCrr
  ): RideTimeEstimate =
    var totalSeconds = 0.0
    var poweredSeconds = 0.0
    var coastingSeconds = 0.0
    var coastingMeters = 0.0
    var modeledMeters = 0.0
    var skippedMeters = 0.0
    var riderEnergyJ = 0.0
    var maxRiderPowerW = 0.0

    // Power tracking is intentionally minimal: V5 candidate-comfort thresholds
    // plus the separate hard-safety wall threshold.
    val trackedPowerThresholdsW = powerThresholdsW(cfg.riderPowerW)
    val currentPowerStreakSeconds =
      mutable.Map.from(trackedPowerThresholdsW.map(_ -> 0.0))
    val longestPowerStreakSeconds =
      mutable.Map.from(trackedPowerThresholdsW.map(_ -> 0.0))
    var candidateComfortSpikeLoadSeconds = 0.0
    var seenCoast = false
    var currentNonCoastSeconds = 0.0
    var leadingNonCoastSeconds = 0.0
    val internalNonCoastRuns = mutable.ArrayBuffer.empty[Double]
    val above = mutable.Map.from(trackedPowerThresholdsW.map(_ -> 0.0))
    if points.size < 2 then
      return RideTimeEstimate(
        totalSeconds = 0.0,
        poweredSeconds = 0.0,
        coastingSeconds = 0.0,
        coastingDistanceKm = 0.0,
        modeledDistanceKm = 0.0,
        skippedDistanceKm = 0.0,
        riderEnergyJ = 0.0,
        maxRiderPowerW = 0.0,
        powerAboveSeconds = above.toMap
      )
    val rawChunks = mutable.ArrayBuffer.empty[(Double, Option[Double])]
    var anchor = points.head
    var accumulatedMeters = 0.0
    var i = 1
    while i < points.size do
      val current = points(i)
      accumulatedMeters += Geometry.distanceMeters(points(i - 1), current)
      val flush = accumulatedMeters >= GradeWindowM || i == points.size - 1
      if flush && accumulatedMeters > 0.0 then
        val delta = (anchor.ele, current.ele) match
            case (Some(z0), Some(z1)) => Some(z1 - z0)
            case _                    => None
        rawChunks += ((accumulatedMeters, delta))
        anchor = current
        accumulatedMeters = 0.0
      i += 1

    // Do not evaluate a tiny final remainder as its own grade window.
    // It is phase-sensitive and can turn one DEM step into a fake power spike.
    val chunks =
      if rawChunks.size >= 2 &&
        rawChunks.last._1 < GradeWindowM * 0.5
      then
        val tail = rawChunks.remove(rawChunks.size - 1)
        val prev = rawChunks.remove(rawChunks.size - 1)
        val mergedDelta = (prev._2, tail._2) match
            case (Some(a), Some(b)) => Some(a + b)
            case _                  => None
        rawChunks += ((prev._1 + tail._1, mergedDelta))
        rawChunks.toVector
      else
        rawChunks.toVector
    chunks.foreach { case (distanceM, deltaOpt) =>
      deltaOpt match
        case Some(deltaM) =>
          val grade = deltaM / distanceM
          val segment = segmentRide(grade, cfg, downhillCapKph, crrOverride)
          val seconds = distanceM / math.max(segment.speedMps, 0.05)
          totalSeconds += seconds
          modeledMeters += distanceM

          trackedPowerThresholdsW.foreach { threshold =>
            if !segment.coasting &&
                segment.riderPowerW > threshold.toDouble + 1e-6
            then
              val next =
                currentPowerStreakSeconds.getOrElse(threshold, 0.0) + seconds
              currentPowerStreakSeconds.update(threshold, next)
              longestPowerStreakSeconds.update(
                threshold,
                math.max(longestPowerStreakSeconds.getOrElse(threshold, 0.0), next)
              )
            else
              currentPowerStreakSeconds.update(threshold, 0.0)
          }

          val candidateSpikeBaseW =
            PowerPolicy.candidateSpikeBaseW(cfg.riderPowerW)
          if !segment.coasting &&
              segment.riderPowerW > candidateSpikeBaseW + 1e-6
          then
            val normalizedExcess =
              (segment.riderPowerW - candidateSpikeBaseW) /
                PowerPolicy.candidateSpikeScaleW(cfg.riderPowerW)
            candidateComfortSpikeLoadSeconds +=
              normalizedExcess * normalizedExcess * seconds

          // Keep non-coasting run boundaries as low-level technical physics
          // diagnostics. They do not participate in trail inclusion or
          // rider-profile selection.
          if segment.coasting then
            if !seenCoast then
              leadingNonCoastSeconds = currentNonCoastSeconds
              seenCoast = true
            else if currentNonCoastSeconds > 0.5 then
              internalNonCoastRuns += currentNonCoastSeconds
            currentNonCoastSeconds = 0.0
          else
            currentNonCoastSeconds += seconds
          if segment.coasting then
            coastingSeconds += seconds
            coastingMeters += distanceM
          else
            poweredSeconds += seconds
            riderEnergyJ += segment.riderPowerW * seconds
            maxRiderPowerW = math.max(maxRiderPowerW, segment.riderPowerW)
            trackedPowerThresholdsW.foreach { threshold =>
              if segment.riderPowerW > threshold.toDouble + 1e-6 then
                above.update(threshold, above.getOrElse(threshold, 0.0) + seconds)
            }
        case None =>
          skippedMeters += distanceM
    }
    val finalLeadingNonCoast =
      if seenCoast then leadingNonCoastSeconds
      else totalSeconds
    val finalTrailingNonCoast =
      if seenCoast then currentNonCoastSeconds
      else totalSeconds
    RideTimeEstimate(
      totalSeconds = totalSeconds,
      poweredSeconds = poweredSeconds,
      coastingSeconds = coastingSeconds,
      coastingDistanceKm = coastingMeters / 1000.0,
      modeledDistanceKm = modeledMeters / 1000.0,
      skippedDistanceKm = skippedMeters / 1000.0,
      riderEnergyJ = riderEnergyJ,
      maxRiderPowerW = maxRiderPowerW,
      powerAboveSeconds = above.toMap,
      leadingNonCoastSeconds = finalLeadingNonCoast,
      trailingNonCoastSeconds = finalTrailingNonCoast,
      internalNonCoastRunsSeconds = internalNonCoastRuns.toVector,
      longestPowerStreakSeconds = longestPowerStreakSeconds.toMap,
      candidateComfortSpikeLoadSeconds = candidateComfortSpikeLoadSeconds
    )
  private case class SegmentRide(speedMps: Double, coasting: Boolean, riderPowerW: Double)

  private def segmentRide(grade: Double, cfg: Config, downhillCapKph: Option[Double], crr: Double): SegmentRide =
    val totalMassKg = cfg.riderWeightKg + cfg.bikeWeightKg
    val downhillCapMps = downhillCapKph.map(_ / 3.6)
    val theta = math.atan(grade)
    val sinTheta = math.sin(theta)
    val cosTheta = math.cos(theta)

    def applyDownhillCap(v: Double): Double =
      downhillCapMps match
        case Some(cap) => math.min(v, cap)
        case None      => v

    val coastDriveForce = -totalMassKg * Gravity * sinTheta -
        crr * totalMassKg * Gravity * cosTheta
    if grade < 0.0 && coastDriveForce > 0.0 then
      val terminal =
        math.sqrt(
          (2.0 * coastDriveForce) /
            (AirDensity * CdA)
        )

      // Downhill semantics:
      // - technical trail GPXs: if gravity can move the bike at all,
      //   the rider does NOT pedal; they only coast/brake to the trail cap.
      // - transfers: keep the existing practical rule that extremely slow
      //   (<2 km/h) natural coasting may be supplemented by pedalling.
      val technicalTrail = downhillCapKph.nonEmpty
      if technicalTrail || terminal >= (2.0 / 3.6) then
        SegmentRide(speedMps = applyDownhillCap(terminal), coasting = true, riderPowerW = 0.0)
      else
        poweredSegment(theta, cfg, applyDownhillCap, crr, capSpeed = downhillCapKph.nonEmpty)
    else
      poweredSegment(theta, cfg, applyDownhillCap, crr, capSpeed = downhillCapKph.nonEmpty)

  private def poweredSegment(
      theta: Double,
      cfg: Config,
      applyDownhillCap: Double => Double,
      crr: Double,
      capSpeed: Boolean
  ): SegmentRide =
    val easySpeed = poweredSpeedForRiderPower(theta, cfg, cfg.riderPowerW, crr)
    val minPedalMps = minPedalingSpeedKph / 3.6

    def maybeCap(v: Double): Double =
      if capSpeed then applyDownhillCap(v) else v
    if easySpeed >= minPedalMps then
      SegmentRide(speedMps = maybeCap(easySpeed), coasting = false, riderPowerW = cfg.riderPowerW)
    else
      // Easy target would force cadence below the conservative 45 rpm
      // climb floor even in 32x51. Rider stays on the bike and increases
      // crank power only enough to keep that low cadence.
      val required = requiredRiderPower(minPedalMps, theta, cfg, crr)
      SegmentRide(speedMps = maybeCap(minPedalMps), coasting = false, riderPowerW = math.max(cfg.riderPowerW, required))

  private def requiredRiderPower(speedMps: Double, theta: Double, cfg: Config, crr: Double): Double =
    val totalMassKg = cfg.riderWeightKg + cfg.bikeWeightKg
    val sinTheta = math.sin(theta)
    val cosTheta = math.cos(theta)
    val gravityAndRolling = totalMassKg * Gravity * sinTheta +
        crr * totalMassKg * Gravity * cosTheta
    val aero = 0.5 * AirDensity * CdA * speedMps * speedMps
    val wheelPower = math.max(0.0, (gravityAndRolling + aero) * speedMps)
    wheelPower / DrivetrainEfficiency

  private def poweredSpeedForRiderPower(theta: Double, cfg: Config, riderPowerW: Double, crr: Double): Double =
    val totalMassKg = cfg.riderWeightKg + cfg.bikeWeightKg
    val wheelPower = riderPowerW * DrivetrainEfficiency
    val sinTheta = math.sin(theta)
    val cosTheta = math.cos(theta)

    def requiredWheelPower(v: Double): Double =
      val gravityAndRolling = totalMassKg * Gravity * sinTheta +
          crr * totalMassKg * Gravity * cosTheta
      val aero = 0.5 * AirDensity * CdA * v * v
      (gravityAndRolling + aero) * v

    var low = 0.05
    var high = 20.0
    var n = 0
    while n < 80 do
      val mid = (low + high) / 2.0
      if requiredWheelPower(mid) <= wheelPower then low = mid
      else high = mid
      n += 1
    low

  def combine(estimates: Iterable[RideTimeEstimate]): RideTimeEstimate =
    val parts = estimates.toVector
    val thresholds =
      parts.flatMap { estimate =>
        estimate.powerAboveSeconds.keys ++ estimate.longestPowerStreakSeconds.keys
      }.distinct.sorted
    val zeroAbove = thresholds.map(_ -> 0.0).toMap
    parts.foldLeft(
      RideTimeEstimate(
        totalSeconds = 0.0,
        poweredSeconds = 0.0,
        coastingSeconds = 0.0,
        coastingDistanceKm = 0.0,
        modeledDistanceKm = 0.0,
        skippedDistanceKm = 0.0,
        riderEnergyJ = 0.0,
        maxRiderPowerW = 0.0,
        powerAboveSeconds = zeroAbove,
        leadingNonCoastSeconds = 0.0,
        trailingNonCoastSeconds = 0.0,
        internalNonCoastRunsSeconds = Vector.empty
      )
    ) { (a, b) =>
      val combinedAbove = thresholds.map { threshold =>
          threshold ->
            (a.abovePowerSeconds(threshold) + b.abovePowerSeconds(threshold))
        }.toMap
      val aHasCoast = a.coastingSeconds > 0.5
      val bHasCoast = b.coastingSeconds > 0.5
      val combinedLeadingNonCoast =
        if aHasCoast then a.leadingNonCoastSeconds
        else a.totalSeconds + b.leadingNonCoastSeconds
      val combinedTrailingNonCoast =
        if bHasCoast then b.trailingNonCoastSeconds
        else b.totalSeconds + a.trailingNonCoastSeconds
      val combinedInternalNonCoast =
        if aHasCoast && bHasCoast then
          a.internalNonCoastRunsSeconds ++
            (if a.trailingNonCoastSeconds + b.leadingNonCoastSeconds > 0.5 then
               Vector(a.trailingNonCoastSeconds + b.leadingNonCoastSeconds)
             else Vector.empty) ++
            b.internalNonCoastRunsSeconds
        else if aHasCoast then
          a.internalNonCoastRunsSeconds
        else if bHasCoast then
          b.internalNonCoastRunsSeconds
        else
          Vector.empty
      RideTimeEstimate(
        totalSeconds = a.totalSeconds + b.totalSeconds,
        poweredSeconds = a.poweredSeconds + b.poweredSeconds,
        coastingSeconds = a.coastingSeconds + b.coastingSeconds,
        coastingDistanceKm = a.coastingDistanceKm + b.coastingDistanceKm,
        modeledDistanceKm = a.modeledDistanceKm + b.modeledDistanceKm,
        skippedDistanceKm = a.skippedDistanceKm + b.skippedDistanceKm,
        riderEnergyJ = a.riderEnergyJ + b.riderEnergyJ,
        maxRiderPowerW = math.max(a.maxRiderPowerW, b.maxRiderPowerW),
        powerAboveSeconds = combinedAbove,
        leadingNonCoastSeconds = combinedLeadingNonCoast,
        trailingNonCoastSeconds = combinedTrailingNonCoast,
        internalNonCoastRunsSeconds = combinedInternalNonCoast,
        longestPowerStreakSeconds =
          thresholds.map { threshold =>
            threshold ->
              math.max(
                a.longestPowerStreakSeconds.getOrElse(threshold, 0.0),
                b.longestPowerStreakSeconds.getOrElse(threshold, 0.0)
              )
          }.toMap,
        candidateComfortSpikeLoadSeconds =
          a.candidateComfortSpikeLoadSeconds + b.candidateComfortSpikeLoadSeconds
      )
    }

object Audit:
  private val OverlapToleranceM = CorridorPolicy.corridorToleranceM

  def run(
      cfg: Config,
      trails: Vector[Trail],
      forbiddenTransferTrails: Vector[Trail],
      finish: Trail,
      order: Vector[Int],
      transitions: Vector[Output.Transition],
      outputPoints: Vector[Point]
  ): AuditResult =
    val warnings = mutable.ArrayBuffer.empty[String]
    val failures = mutable.ArrayBuffer.empty[String]
    val selected = order.map(trails)
    val allInputTrails = trails.distinctBy(_.path)
    val forbiddenTransferPaths = forbiddenTransferTrails.map(_.path).toSet
    val allProtectedTrails = (allInputTrails ++ forbiddenTransferTrails).distinctBy(_.path)
    selected.foreach { trail =>
      val stats = Geometry.profileStats(trail.points)
      val (forwardFound, reversedFound) =
        Geometry.sampleMatchDirection(outputPoints, trail.points)
      val isFinal = trail.path == finish.path
      val direction =
        if isFinal then "FIXED FINAL"
        else
          stats.netElevationM match
            case Some(net) if net <= -5.0 => "DOWNHILL"
            case Some(net) if net >= 5.0  => "UPHILL"
            case Some(_)                  => "MIXED/FLAT"
            case None                     => "NO-ELEVATION"
      if !isFinal && direction == "UPHILL" then
        failures += s"${trail.name}: GPX net direction is uphill"
      else if !isFinal && direction == "MIXED/FLAT" then
        warnings += s"${trail.name}: GPX is not clearly downhill by net elevation"
      if !forwardFound then
        failures += s"${trail.name}: forward occurrence was not verified in final GPX"
      if reversedFound then
        warnings += s"${trail.name}: reversed sample sequence also appears somewhere in final GPX"
      if !isFinal &&
          stats.longestClimbDistanceM >= 100.0 &&
          stats.longestClimbAvgGradePct >= 3.0
      then
        warnings +=
          f"${trail.name}: contains ${stats.longestClimbDistanceM}%.0f m sustained climb at ~${stats.longestClimbAvgGradePct}%.1f%%"
    }
    transitions.foreach { transition =>
      val c = transition.connector
      val stats = Geometry.profileStats(c.route.points)
      val fromSnap = c.route.points.headOption
        .map(Geometry.distanceMeters(c.route.from, _))
        .getOrElse(Double.PositiveInfinity)
      val toSnap = c.route.points.lastOption
        .map(Geometry.distanceMeters(c.route.to, _))
        .getOrElse(Double.PositiveInfinity)
      if fromSnap > 30.0 then
        warnings += f"${transition.label}: route starts ${fromSnap}%.0f m from requested endpoint"
      if toSnap > 30.0 then
        warnings += f"${transition.label}: route ends ${toSnap}%.0f m from requested endpoint"

      case class AuditOverlap(trail: Trail, meters: Double, strictForbidden: Boolean)
      val overlaps = allProtectedTrails.map { trail =>
          AuditOverlap(
            trail,
            Geometry.coTravelOverlapMeters(c.route.points, trail.points, OverlapToleranceM),
            forbiddenTransferPaths.contains(trail.path)
          )
        }.filter(_.meters >= 5.0).sortBy(o => -o.meters)
      overlaps.foreach { overlap =>
        if overlap.meters > CorridorPolicy.maxConnectorCoTravelM then
          if overlap.strictForbidden then
            failures +=
              f"${transition.label}: connector rides forbidden transfer corridor ${overlap.trail.name} for ${overlap.meters}%.0f m"
          else
            failures +=
              f"${transition.label}: connector reuses mandatory technical corridor ${overlap.trail.name} for ${overlap.meters}%.0f m"
        else if overlap.strictForbidden then
          warnings +=
            f"${transition.label}: connector briefly co-travels forbidden transfer corridor ${overlap.trail.name} for ${overlap.meters}%.0f m"
        else
          warnings +=
            f"${transition.label}: connector briefly co-travels mandatory technical corridor ${overlap.trail.name} for ${overlap.meters}%.0f m"
      }
      if stats.longestClimbDistanceM >= 150.0 && stats.longestClimbAvgGradePct >= 4.0 then
        warnings +=
          f"${transition.label}: long connector climb ${stats.longestClimbDistanceM}%.0f m at ~${stats.longestClimbAvgGradePct}%.1f%%"
      if stats.maxGrade100Pct >= 8.0 then
        warnings +=
          f"${transition.label}: max sustained 100 m uphill grade ${stats.maxGrade100Pct}%.1f%%"
      val edges = c.traceEdges
      require(
        edges.nonEmpty,
        s"${transition.label}: selected connector lost required cached trace edge evidence"
      )
      val badUses = edges.filter(e => Set("steps", "ferry", "rail-ferry", "rail")(e.use))
      val badSurfaces = edges.filter(_.surface == "impassable")
      val majorRoads = edges.filter(e => Set("motorway", "trunk", "primary")(e.roadClass))
      def cycleLaneKind(e: TraceEdge): String =
        e.cycleLane.trim.toLowerCase match
          case "" | "no" | "unknown" | "false" => "none"
          case other                            => other

      val primaryNoCycle = edges.filter(e => e.roadClass == "primary" && cycleLaneKind(e) == "none")
      val primaryShared = edges.filter(e => e.roadClass == "primary" && cycleLaneKind(e) == "shared")
      if badUses.nonEmpty then
        failures +=
          s"${transition.label}: edge audit contains ${badUses.map(_.use).distinct.mkString(", ")}"
      if badSurfaces.nonEmpty then
        failures += s"${transition.label}: edge audit contains impassable surface"
      if majorRoads.exists(e => Set("motorway", "trunk")(e.roadClass)) then
        failures += s"${transition.label}: connector uses motorway/trunk edge"
      else if primaryNoCycle.nonEmpty || primaryShared.nonEmpty then
        warnings +=
          s"${transition.label}: connector uses primary-road edge without dedicated/separated cycle facility"
    }
    val startGap = outputPoints.headOption
      .map(Geometry.distanceMeters(cfg.start, _))
      .getOrElse(Double.PositiveInfinity)
    val endGap = outputPoints.lastOption
      .map(Geometry.distanceMeters(finish.end, _))
      .getOrElse(Double.PositiveInfinity)
    val actualMaxGap = Geometry.profileStats(outputPoints).maxPointGapM
    if startGap > 5.0 then
      failures += f"Output GPX starts ${startGap}%.1f m away from requested START"
    if endGap > 5.0 then
      failures += f"Output GPX ends ${endGap}%.1f m away from requested ${finish.name} point"
    if actualMaxGap >= 250.0 then
      failures +=
        f"Output GPX has a ${actualMaxGap}%.1f m consecutive point gap (>=250 m data-quality limit)"
    else if actualMaxGap >= 100.0 then
      warnings +=
        f"Output GPX has a ${actualMaxGap}%.1f m consecutive point gap; inspect sparse technical geometry"
    AuditResult(warnings = warnings.toVector.distinct, failures = failures.toVector.distinct)

object Output:
  case class Transition(label: String, connector: Connector)

  def assemble(
      cfg: Config,
      trails: Vector[Trail],
      startLabel: String,
      finish: Trail,
      matrix: Matrix,
      order: Vector[Int]
  ): (Vector[Point], Vector[Transition]) =
    val points = mutable.ArrayBuffer.empty[Point]
    val transitions = mutable.ArrayBuffer.empty[Transition]

    def append(chunk: Vector[Point]): Unit =
      val merged = Geometry.appendDistinct(points.toVector, chunk)
      points.clear()
      points ++= merged
    if order.isEmpty then
      val c = matrix.startToFinish.getOrElse(sys.error("Missing endpoint-only connector"))
      append(Vector(c.route.from))
      transitions += Transition(s"$startLabel -> ${finish.name}", c)
      append(c.route.points)
    else
      val first = order.head
      val firstConnector = matrix.startToTrail(first).getOrElse(sys.error("Missing endpoint -> first connector"))
      append(Vector(firstConnector.route.from))
      transitions += Transition(s"$startLabel -> ${trails(first).name}", firstConnector)
      append(firstConnector.route.points)
      append(trails(first).points)
      order.sliding(2).foreach {
        case Vector(a, b) =>
          val c = matrix.between(a)(b).getOrElse {
            sys.error(s"Missing connector ${trails(a).name} -> ${trails(b).name}")
          }
          transitions += Transition(s"${trails(a).name} -> ${trails(b).name}", c)
          append(c.route.points)
          append(trails(b).points)
        case _ =>
      }
      val last = order.last
      val finalConnector = matrix.trailToFinish(last).getOrElse(sys.error(s"Missing last -> ${finish.name} connector"))
      transitions += Transition(s"${trails(last).name} -> ${finish.name}", finalConnector)
      append(finalConnector.route.points)
    (points.toVector, transitions.toVector)

  def withVariantSuffix(path: Path, suffix: String): Path =
    if suffix.isEmpty then path
    else
      val name = path.getFileName.toString
      val dot = name.lastIndexOf('.')
      val variantName =
        if dot > 0 then name.substring(0, dot) + suffix + name.substring(dot)
        else name + suffix
      val parent = Option(path.getParent).getOrElse(Path.of("."))
      parent.resolve(variantName)

  def defaultDebugReportPath(report: Path): Path =
    val name = report.getFileName.toString
    val base =
      if name.toLowerCase.endsWith(".txt") then name.dropRight(4)
      else name
    val parent = Option(report.getParent).getOrElse(Path.of("."))
    parent.resolve(base + ".debug.txt")


  def defaultReportPath(out: Path): Path =
    val name = out.getFileName.toString
    val base =
      if name.toLowerCase.endsWith(".gpx") then name.dropRight(4)
      else name
    val parent = Option(out.getParent).getOrElse(Path.of("."))
    parent.resolve(base + ".txt")


object OldContractTests:
  case class Summary(passed: Int, failed: Int)

  private def assertT(condition: Boolean, message: => String = "assertion failed"): Unit =
    if !condition then throw new AssertionError(message)

  private def near(a: Double, b: Double, tolerance: Double): Unit =
    assertT(math.abs(a - b) <= tolerance, f"$a%.6f != $b%.6f within $tolerance%.6f")

  def run(): Summary =
    var passed = 0
    var failed = 0

    def test(name: String)(body: => Unit): Unit =
      try
        body
        passed += 1
        println(s"PASS contract: $name")
      catch
        case e: Throwable =>
          failed += 1
          println(s"FAIL contract: $name: ${Option(e.getMessage).getOrElse(e.getClass.getSimpleName)}")

    val origin = Point(53.0, 10.0, None)
    val earthR = 6371000.0
    val cos0 = math.cos(math.toRadians(origin.lat))
    def metres(x: Double, y: Double): Point =
      Point(
        origin.lat + math.toDegrees(y / earthR),
        origin.lon + math.toDegrees(x / (earthR * cos0)),
        None
      )

    test("protected corridor parallel/reverse") {
      val corridor = Vector(metres(-20, 0), metres(50, 0))
      val forward = Vector(metres(0, 0), metres(30, 0))
      val reverse = forward.reverse
      val a = Geometry.coTravelOverlapMeters(forward, corridor, 12.0)
      val b = Geometry.coTravelOverlapMeters(reverse, corridor, 12.0)
      assertT(a > 29.0, s"parallel overlap=$a")
      near(a, b, 0.05)
    }

    test("protected corridor crossing ignored") {
      val corridor = Vector(metres(-20, 0), metres(50, 0))
      val perpendicular = Vector(metres(10, -20), metres(10, 20))
      val oblique = Vector(metres(-2, -14), metres(26, 14))
      assertT(Geometry.coTravelOverlapMeters(perpendicular, corridor, 12.0) < 1e-6)
      assertT(Geometry.coTravelOverlapMeters(oblique, corridor, 12.0) < 1e-6)
    }

    test("protected corridor 12m boundary") {
      val corridor = Vector(metres(-20, 0), metres(50, 0))
      val outside = Vector(metres(0, 12.1), metres(30, 12.1))
      val inside = Vector(metres(0, 11.9), metres(30, 11.9))
      assertT(Geometry.coTravelOverlapMeters(outside, corridor, 12.0) < 1e-6)
      assertT(Geometry.coTravelOverlapMeters(inside, corridor, 12.0) > 29.0)
    }

    test("protected corridor blocker is interior") {
      val corridor = Vector(metres(-20, 0), metres(50, 0))
      val route = Vector(metres(0, 6), metres(30, 6))
      val blocker = Geometry.coTravelBlockPoint(route, corridor, 12.0, route.head, route.last)
      assertT(blocker.nonEmpty)
      val depth = math.min(
        Geometry.distanceMeters(blocker.get, route.head),
        Geometry.distanceMeters(blocker.get, route.last)
      )
      assertT(depth > 5.0, s"blocker depth=$depth")
    }

    test("corridor rectangle intersection stays empty") {
      // Regression for the FIX26 bug: max(lower bounds) > min(upper bounds)
      // must remain an empty intersection rather than being re-ordered by add().
      val route = Vector(
        metres(0, 0), metres(10, -20), metres(-20, -50),
        metres(-15, -50), metres(-5, -70)
      )
      val corridor = Vector(
        metres(0, 0), metres(-5, 5), metres(15, 0),
        metres(5, 30), metres(25, 60)
      )
      val overlap = Geometry.coTravelOverlapMeters(route, corridor, 12.0)
      assertT(overlap < 1e-6, f"false-positive overlap=$overlap%.3fm")
    }

    test("exact blocker accumulation is monotone") {
      val a = metres(1, 1)
      val b = metres(2, 2)
      val first = Geometry.appendExactDistinctPoints(Vector.empty, Vector(a))
      val second = Geometry.appendExactDistinctPoints(first, Vector(a, b))
      assertT(first == Vector(a))
      assertT(second == Vector(a, b))
    }

    val summary = Summary(passed, failed)
    println(s"CONTRACT TESTS: ${summary.passed} passed, ${summary.failed} failed")
    summary

@main def main(args: String*): Unit =
  val timingRunStartedNs = System.nanoTime()

  def timingSeconds(fromNs: Long, toNs: Long): Double =
    (toNs - fromNs).toDouble / 1e9

  val cfg = Cli.parse(args) match
    case Right(Some(value)) => value
    case Right(None)        => return
    case Left(message) =>
      Console.err.println(s"Error: $message")
      return

  if cfg.runTests then
    val contract = OldContractTests.run()
    if contract.failed > 0 then
      Console.err.println(s"ERROR: ${contract.failed} contract test(s) failed")
      return
  else
    println("CONTRACT TESTS: skipped (--no-test)")

  println(s"Planner build: ${BuildInfo.id}")
  println("PRODUCT TARGET: three natural rider day variants (two LOOP + one P2P); wall ceilings may repeat; every technical GPX remains mandatory independently.")
  println()

  val endpoint1 = cfg.start

  val endpoint2 = cfg.finish

  val endpointPoints = Vector(endpoint1, endpoint2)

  val endpointNames = Vector(cfg.startName, cfg.finishName)

  def endpointNumber(point: Point): Int =
    val distances =
      endpointPoints.map { endpoint =>
          Geometry.distanceMeters(point, endpoint)
      }
    distances.zipWithIndex.minBy(
      _._1
    )._2 + 1

  def endpointName(point: Point): String =
    endpointNames(endpointNumber(point) - 1)

  def endpointTrail(number: Int, point: Point, name: String): Trail =
    Trail(
      path =
        Path.of(
          s"__endpoint_${number}__"
        ),
      name = name,
      points = Vector(point)
    )

  val endpointTrail1 = endpointTrail(1, endpoint1, cfg.startName)

  val endpointTrail2 = endpointTrail(2, endpoint2, cfg.finishName)

  val baseReportPath = cfg.report.getOrElse(Output.defaultReportPath(cfg.out))

  val debugPath = Output.defaultDebugReportPath(baseReportPath)


  // Rider-facing output names are fixed product slots: cfg.out is C1,
  // while C2/C3 use wall suffixes.
  val wallC2GpxPath = Output.withVariantSuffix(cfg.out, ".wall-c2")

  val wallC3GpxPath = Output.withVariantSuffix(cfg.out, ".wall-c3")

  val generatedOutputPaths =
    Set(
      cfg.out,
      wallC2GpxPath,
      wallC3GpxPath
    ).map(_.toAbsolutePath.normalize)

  val expanded = Inputs.expand(cfg.inputs)

  val realRidePaths = RealRideGpx.defaultPaths()

  val forbiddenTransferPaths = ForbiddenTransferInputs.defaultPaths()

  val realRidePathSet =
    realRidePaths.map(
      _.toAbsolutePath.normalize
    ).toSet

  val forbiddenTransferPathSet =
    forbiddenTransferPaths.map(
      _.toAbsolutePath.normalize
    ).toSet

  val ordinaryPaths = expanded.filterNot { p =>
      val normalized = p.toAbsolutePath.normalize
      generatedOutputPaths.contains(
        normalized
      ) ||
        realRidePathSet.contains(
          normalized
        ) ||
        forbiddenTransferPathSet.contains(normalized)
    }

  val rawTrails = ordinaryPaths.map(Gpx.read)

  // Every technical GPX is an independent mandatory trail. Shared geometry
  // is reported and validated, but never changes which input GPXs must be ridden.
  val sourceTrails = rawTrails

  val trails = sourceTrails

  val trailCount = trails.size

  val forbiddenTransferTrails = forbiddenTransferPaths.map(Gpx.read)

  // Real-ride evidence changes production wall severity, so it must not
  // disappear silently. Until this is replaced by a versioned precomputed
  // evidence artifact, the current production contract requires the dual-
  // recording trails/real set to be present and every discovered GPX readable.
  require(
    realRidePaths.size >= 2,
    s"Safety-active real-ride evidence requires >=2 trails/real/*.gpx recordings; found ${realRidePaths.size}."
  )

  val realRides = realRidePaths.map { path =>
      try
        RealRideGpx.read(path)
      catch
        case e: Exception =>
          throw new IllegalArgumentException(
            s"Safety-active real-ride evidence GPX is unreadable: $path: ${Option(e.getMessage).getOrElse(e.getClass.getSimpleName)}",
            e
          )
    }
  println(
    s"Real-ride evidence: ${realRides.size}/${realRidePaths.size} required GPX(s) validated from trails/real/; " +
      s"${realRides.map(_.samples.map(_.segmentIndex).distinct.size).sum} continuity segment(s); " +
      "these are safety-active evidence only, not mandatory technical trails."
  )
  println()

  if forbiddenTransferTrails.nonEmpty then
    println(
      s"Forbidden transfer corridors: ${forbiddenTransferTrails.size} GPX(s) from trails/avoid/; " +
        "these are never mandatory ride trails and connectors are rerouted away from their full geometry."
    )
    forbiddenTransferTrails.foreach { trail =>
      println(
        s"  AVOID: ${trail.name} (${trail.path})"
      )
    }
    println()
  require(trails.nonEmpty, "No technical GPX trails found.")
  require(
    trails.size <= 30,
    s"Exact bit-mask search currently supports at most 30 mandatory GPXs; found ${trails.size}."
  )

  val CanonicalMaxPointGapM = 10.0
  trails.foreach { trail =>
    val gap =
      Geometry.profileStats(
        trail.points
      ).maxPointGapM
    require(
      gap <= CanonicalMaxPointGapM + 1e-9,
      f"Production technical GPX violates canonical geometry contract: ${trail.name}%s max point gap=$gap%.1f m, required <=$CanonicalMaxPointGapM%.1f m."
    )
  }
  println(
    s"${cfg.startName}: ${Format.point(endpoint1)}"
  )
  println(
    s"${cfg.finishName}: ${Format.point(endpoint2)}"
  )
  println(
    s"Allowed endpoint modes: ${cfg.startName}->${cfg.startName}, ${cfg.startName}->${cfg.finishName}, ${cfg.finishName}->${cfg.startName}, ${cfg.finishName}->${cfg.finishName}"
  )
  println(
    s"Technical trails: ${trails.size}"
  )
  println(
    f"Mandatory input contract: directory expansion is non-recursive; GPX endpoints are exact connector join boundaries; full mandatory geometry is protected; max point gap <=$CanonicalMaxPointGapM%.1f m."
  )
  trails.zipWithIndex.foreach {
    case (trail, idx) =>
      println(
        f"  ${idx + 1}%2d. ${trail.name}%-32s " +
          s"${Format.point(trail.start)} -> ${Format.point(trail.end)}"
      )
  }
  println()

  val valhalla = Valhalla(cfg.valhalla, cfg.profile, cfg.physicsSampleM)

  val fingerprint = valhalla.statusFingerprint()
  println(s"Valhalla: $fingerprint")

  // Mandatory trails and explicit transfer-avoid corridors are different
  // product concepts:
  //   trails/*.gpx       = must ride
  //   trails/avoid/*.gpx = must NOT be used as connector corridors
  //
  // Both are protected from connector reuse, but only the first set enters the
  // all-trails solver.
  val safetyReservedTrails =
    (
      trails ++
        forbiddenTransferTrails
    ).distinctBy(_.path)
  println()
  println()

  val cache = RouteCache()

  val matrixPoint1 =
    MatrixBuilder.build(
      endpoint1,
      trails,
      endpointTrail1,
      cache,
      valhalla,
      cfg,
      safetyReservedTrails,
      forbiddenTransferTrails
    )

  val matrixPoint2 =
    MatrixBuilder.build(
      endpoint2,
      trails,
      endpointTrail2,
      cache,
      valhalla,
      cfg,
      safetyReservedTrails,
      forbiddenTransferTrails,
      buildBetween = false
    )

  def flattenedSearchVariants(connector: Connector): Vector[Connector] =
    if connector.searchConnectorVariants.nonEmpty then connector.searchConnectorVariants
    else Vector(connector)

  def mergeEndpointChoices(a: Option[Connector], b: Option[Connector]): Option[Connector] =
    val candidates =
      (
        a.toVector.flatMap(
          flattenedSearchVariants
        ) ++
          b.toVector.flatMap(flattenedSearchVariants)
      ).sortBy { connector =>
        (
          connector.physicsSeconds,
          connector.candidateComfortPenaltySeconds,
          connector.transferQualityPenaltySeconds,
          connector.fatiguePenaltySeconds,
          connector.pathFraction
        )
      }
    candidates.headOption.map { best =>
      best.copy(searchConnectorVariants = candidates)
    }

  val matrix =
    Matrix(
      startToTrail =
        trails.indices.map(
          i =>
            mergeEndpointChoices(matrixPoint1.startToTrail(i), matrixPoint2.startToTrail(i))
        ).toVector,
      between = matrixPoint1.between,
      trailToFinish =
        trails.indices.map(
          i =>
            mergeEndpointChoices(matrixPoint1.trailToFinish(i), matrixPoint2.trailToFinish(i))
        ).toVector,
      startToFinish = None
    )

  val timingMatrixReadyNs = System.nanoTime()
  println(
    s"[1/4 100.0%] Connector matrix complete: start and end independently choose ${cfg.startName} or ${cfg.finishName}."
  )
  println(
    s"Explicit /avoid runtime audit: loaded=${forbiddenTransferTrails.size}"
  )
  if forbiddenTransferTrails.nonEmpty then
    println(
      "  strict /avoid rerouting uses blocker points taken from the actual connector geometry that violated the hard co-travel audit; no raw-coordinate junction inference or fixed endpoint-clearance radius is used; sustained co-travel >12 m is FAIL."
    )
  println()

  def writeText(path: Path, content: String): Unit =
    Option(
      path.getParent
    ).foreach { p =>
        Files.createDirectories(p)
    }
    Files.writeString(path, content, StandardCharsets.UTF_8)

  def writeEarlyFailureReports(reason: String, phase: String): Unit =
    val summary = new StringBuilder
    summary.append(
      "TRAIL-PLAN — NO RIDER GPX PRODUCED\n" +
        "===================================\n\n" +
        s"Planner build: ${BuildInfo.id}\n" +
        s"Phase: $phase\n" +
        s"Reason: $reason\n\n" +
        "No day GPX was written because the all-mandatory-trails route is incomplete. " +
        "This report and the deep diagnostic are intentionally written even on failure so every run leaves something concrete to test/debug.\n\n" +
        s"Mandatory technical GPXs: $trailCount.\n" +
        s"Explicit trails/avoid: loaded=${forbiddenTransferTrails.size}\n\n" +
        "REACHABILITY SUMMARY\n" +
        "--------------------\n"
    )
    trails.indices.foreach { i =>
      val starts = matrix.startToTrail(i).size
      val inbound = trails.indices.filter(_ != i).map(j => matrix.between(j)(i).size).sum
      val outbound = trails.indices.filter(_ != i).map(j => matrix.between(i)(j).size).sum
      val ends = matrix.trailToFinish(i).size
      summary.append(
        f"${trails(i).name}%-32s start=$starts%3d inbound=$inbound%4d outbound=$outbound%4d end=$ends%3d\n"
      )
    }
    summary.append(
      "\nNext action: inspect day.debug.txt. Zero inbound usually means a target-entry topology/safety cut; zero outbound means an exit cut.\n"
    )
    val deep = new StringBuilder
    deep.append(summary.result())
    deep.append("\n")
    deep.append(
      "\nTARGET-START PROXIMITY FOR UNREACHABLE ENTRIES\n" +
        "----------------------------------------------\n"
    )
    trails.indices.foreach { i =>
      val starts = matrix.startToTrail(i).size
      val inbound = trails.indices.filter(_ != i).map(j => matrix.between(j)(i).size).sum
      if starts == 0 || inbound == 0 then
        val target = trails(i)
        deep.append(s"${target.name}:\n")
        trails.indices
          .filter(_ != i)
          .flatMap { j =>
            Geometry.projectToPolyline(target.start, trails(j).points)
              .map(p => (trails(j).name, p.alongM, p.lateralM))
          }
          .sortBy(_._3)
          .take(6)
          .foreach { case (name, alongM, lateralM) =>
            val mark =
              if lateralM <= CorridorPolicy.corridorToleranceM then
                " [near mandatory geometry; no synthetic topology rescue is used]"
              else
                ""
            deep.append(f"  $name%-32s lateral=$lateralM%6.1f m | along-from-parent-start=$alongM%7.1f m$mark%s\n")
          }
        forbiddenTransferTrails
          .flatMap { t =>
            Geometry.projectToPolyline(target.start, t.points)
              .map(p => (t.name, p.alongM, p.lateralM))
          }
          .sortBy(_._3)
          .take(6)
          .foreach { case (name, alongM, lateralM) =>
            val mark =
              if lateralM <= 6.0 then
                " [near protected /avoid geometry; route-derived blockers are used only after an actual >12 m violation]"
              else if lateralM <= 12.0 then
                " [NEAR AVOID; proximity alone is allowed, sustained co-travel is forbidden]"
              else
                ""
            deep.append(f"  AVOID $name%-26s lateral=$lateralM%6.1f m | along=$alongM%7.1f m$mark%s\n")
          }
        deep.append("\n")
    }
    writeText(baseReportPath, summary.result())
    writeText(debugPath, deep.result())
    Console.err.println(
      s"Failure reports written: ${baseReportPath.toAbsolutePath.normalize} and ${debugPath.toAbsolutePath.normalize}"
    )

  def inboundCount(i: Int): Int =
    matrix.startToTrail(i).size +
      trails.indices.count(
        j =>
          j != i &&
            matrix.between(j)(i).nonEmpty
      )

  def outboundCount(i: Int): Int =
    matrix.trailToFinish(i).size +
      trails.indices.count(
        j =>
          j != i &&
            matrix.between(i)(j).nonEmpty
      )

  val reachabilityNotes = trails.indices
      .filter { i =>
          inboundCount(i) == 0 ||
            outboundCount(i) == 0
      }
      .map { i =>
        s"${trails(i).name}: inbound=${inboundCount(i)}, outbound=${outboundCount(i)}"
      }
      .toVector

  if reachabilityNotes.nonEmpty then
    println("Reachability notes:")
    reachabilityNotes.foreach { x =>
        println(s"  $x")
    }
    println()

  // Canonical technical GPX contract:
  // every mandatory technical GPX already contains the exact geometry and
  // elevation profile used for rider physics. No runtime source arbitration,
  // Valhalla elevation fallback or endpoint reanchoring remains here.
  trails.foreach { trail =>
    require(
      trail.points.forall(
        _.ele.isDefined
      ),
      s"Production technical GPX must contain canonical <ele> on every point: ${trail.path}"
    )
  }
  println(
    s"[2/4 0.0%] Using canonical GPX geometry/elevation directly for ${trails.size} trail(s)."
  )

  val technicalTrailPoints = trails.zipWithIndex.map { case (trail, idx) =>
      val pct = 80.0 * (idx + 1).toDouble / math.max(1, trails.size).toDouble
      println(
        f"[2/4 $pct%5.1f%%] canonical elevation ${idx + 1}%d/${trails.size}%d ${trail.name}%s"
      )
      trail.points
    }
  println(
    s"Technical trail elevation source: production canonical GPX <ele> (${trails.size}/${trails.size})."
  )
  println()

  val technicalTrailTimes = technicalTrailPoints.map { points =>
      RidePhysics.estimate(
        points,
        cfg,
        downhillCapKph = Some(cfg.downhillMaxKph)
      )
    }

  val technicalTrailStats = technicalTrailPoints.map(Geometry.profileStats)

  val technicalTrailSinuosity =
    trails.map { trail =>
        Geometry.sinuosity(trail.points)
    }

  val technicalTrailNetDescentPct = technicalTrailStats.map { stats =>
      if stats.lengthM <= 1e-9 then 0.0
      else stats.netElevationM
          .map { net =>
              math.max(
                0.0,
                -net
              ) /
                stats.lengthM *
                100.0
          }
          .getOrElse(0.0)
    }

  val technicalTrailLateAscentM =
    technicalTrailPoints.map { points =>
        Geometry.exponentiallyWeightedAscentNearEnd(points, cfg.arrivalWindowM)
    }

  case class LocalSteepTwistyHit(centerM: Double, netDescentPct: Double, sinuosity: Double, ruleName: String):
    def severity: Double =
      val descentThreshold =
        if ruleName == "60m" then RidePhysics.NoviceSteepTwistyLocalShortMinNetDescentPct
        else RidePhysics.NoviceSteepTwistyLocalLongMinNetDescentPct
      math.min(
        netDescentPct /
          descentThreshold,
        sinuosity /
          RidePhysics.NoviceSteepTwistyLocalMinSinuosity
      )

  val technicalTrailLocalShortWindows =
    technicalTrailPoints.map { points =>
        Geometry.descentSinuosityWindows(
          points,
          RidePhysics.NoviceSteepTwistyLocalShortWindowM,
          RidePhysics.NoviceSteepTwistyLocalShortMinNetDescentPct,
          RidePhysics.NoviceSteepTwistyLocalMinSinuosity
        )
    }

  val technicalTrailLocalLongWindows =
    technicalTrailPoints.map { points =>
        Geometry.descentSinuosityWindows(
          points,
          RidePhysics.NoviceSteepTwistyLocalLongWindowM,
          RidePhysics.NoviceSteepTwistyLocalLongMinNetDescentPct,
          RidePhysics.NoviceSteepTwistyLocalMinSinuosity
        )
    }

  val technicalTrailLocalSteepTwistyHit = trails.indices.map { i =>
      val shortHits = technicalTrailLocalShortWindows(i)
          .filter { window =>
            window.netDescentPct >= RidePhysics.NoviceSteepTwistyLocalShortMinNetDescentPct &&
              window.sinuosity >= RidePhysics.NoviceSteepTwistyLocalMinSinuosity
          }
          .map { window =>
            LocalSteepTwistyHit(window.centerM, window.netDescentPct, window.sinuosity, "60m")
          }
      val longHits = technicalTrailLocalLongWindows(i)
          .filter { window =>
            window.netDescentPct >= RidePhysics.NoviceSteepTwistyLocalLongMinNetDescentPct &&
              window.sinuosity >= RidePhysics.NoviceSteepTwistyLocalMinSinuosity
          }
          .map { window =>
            LocalSteepTwistyHit(window.centerM, window.netDescentPct, window.sinuosity, "100m")
          }
      (shortHits ++ longHits)
        .sortBy(
          hit =>
            (-hit.severity, -hit.netDescentPct, -hit.sinuosity, hit.centerM)
        )
        .headOption
    }.toVector

  val noviceSteepTwistyWholeIndices = trails.indices
      .filter { i =>
        technicalTrailNetDescentPct(i) >= RidePhysics.NoviceSteepTwistyMinNetDescentPct &&
        technicalTrailSinuosity(i) >= RidePhysics.NoviceSteepTwistyMinSinuosity
      }
      .toSet

  val noviceSteepTwistyLocalIndices = trails.indices
      .filter { i =>
          technicalTrailLocalSteepTwistyHit(i).nonEmpty
      }
      .toSet

  // Every GPX remains mandatory. Whole-trail or strong local 60/100 m
  // evidence marks a trail as demanding; ordering later inserts recovery
  // separators instead of dropping it.
  val noviceSteepTwistyIndices = noviceSteepTwistyWholeIndices |
      noviceSteepTwistyLocalIndices

  // Every GPX is independent, so demanding/recovery classification is based
  // only on that trail's own geometry/elevation model. No sibling-based
  // inheritance or difficulty-tier substitution exists.
  val effectiveSteepTwistyIndices = noviceSteepTwistyIndices

  // ---------------------------------------------------------------------
  // Wall classes + compact all-trails Held-Karp
  //
  // Ordering still uses (visited subset mask, last ridden trail), but the
  // rider-facing solver below no longer keeps an open Pareto frontier there.
  // Connector alternatives remain available and are cached once before the
  // three wall-ceiling solves.
  // ---------------------------------------------------------------------

  case class ClimbShape(valuesM: Vector[Double], maxAscentM: Double, upwardViolationM: Double, roughnessM: Double, totalAscentM: Double):
    def lastAscentM: Option[Double] =
      valuesM.lastOption

    def lastDeltaM: Option[Double] =
      if valuesM.size >= 2 then
        Some(
          valuesM.last -
            valuesM(valuesM.size - 2)
        )
      else
        None

  // Keep these as ordinary local defs rather than a local companion object.
  // Scala 3/GraalVM can recursively initialize a local companion accessor here,
  // which was the source of the previous StackOverflowError.
  def emptyClimbShape: ClimbShape =
    ClimbShape(valuesM = Vector.empty, maxAscentM = 0.0, upwardViolationM = 0.0, roughnessM = 0.0, totalAscentM = 0.0)

  def appendClimbShape(base: ClimbShape, ascentM: Double): ClimbShape =
    val ascent = math.max(0.0, ascentM)
    val previous = base.lastAscentM
    val delta =
      previous.map { p =>
          ascent - p
      }
    val addedUpwardViolation =
      previous.map(
        p =>
          math.max(0.0, ascent - p)
      ).getOrElse(0.0)
    val addedRoughness =
      (
        base.lastDeltaM,
        delta
      ) match
        case (Some(prevDelta), Some(nextDelta)) =>
          math.abs(nextDelta - prevDelta)
        case _ =>
          0.0
    ClimbShape(
      valuesM =
        base.valuesM :+
          ascent,
      maxAscentM =
        math.max(
          base.maxAscentM,
          ascent
        ),
      upwardViolationM =
        base.upwardViolationM +
          addedUpwardViolation,
      roughnessM =
        base.roughnessM +
          addedRoughness,
      totalAscentM =
        base.totalAscentM +
          ascent
    )

  case class MultiLabelRoute(
      mask: Int,
      order: Vector[Int],
      connectors: Vector[Connector],
      movingSeconds: Double,
      candidateComfortSufferingSeconds: Double,
      candidateLongestLowSeconds: Double,
      candidateLongestHighSeconds: Double,
      candidateSpikeLoadSeconds: Double,
      downhillHandlingSeconds: Double,
      roadStressSeconds: Double,
      recoveryBurdenSeconds: Double,
      pathBurdenSeconds: Double,
      maxConnectorGrade100Pct: Double,
      maxRiderPowerW: Double,
      maxWallClass: Int,
      climbShape: ClimbShape,
      trailLateAscentBurden: Double
  )

  def connectorAbovePowerSeconds(connector: Connector, thresholdW: Int): Double =
    connector.powerAboveSecondsByThreshold.getOrElse(thresholdW, 0.0)

  def connectorLongestPowerStreakSeconds(
      connector: Connector,
      thresholdW: Int
  ): Double =
    connector.longestPowerStreakSecondsByThreshold.getOrElse(thresholdW, 0.0)

  def connectorCandidateLongestLow(connector: Connector): Double =
    val threshold =
      PowerPolicy.candidateStopStreakThresholdsW(cfg.riderPowerW).head
    connectorLongestPowerStreakSeconds(connector, threshold)

  def connectorCandidateLongestHigh(connector: Connector): Double =
    val threshold =
      PowerPolicy.candidateStopStreakThresholdsW(cfg.riderPowerW).last
    connectorLongestPowerStreakSeconds(connector, threshold)

  // Production road stress means traffic exposure without a dedicated or
  // separated bicycle facility:
  //   - motorway/trunk;
  //   - primary: no cycle lane OR only "shared";
  //   - secondary: no cycle lane OR only "shared".
  //
  // A Valhalla "shared" cycle lane is intentionally not promoted to the safe
  // bucket: it is below dedicated/separated in Valhalla's own lane hierarchy
  // and can represent shared-lane / share-busway semantics.
  //
  // V4-10 still does not add a new hard road ban. It corrects the meaning of
  // the road signal first so the next safety decision is based on the right
  // exposure.
  def connectorRoadStress(connector: Connector): Double =
    connector.motorwayTrunkSeconds +
      connector.primaryNoCycleSeconds +
      connector.primarySharedSeconds +
      connector.secondaryNoCycleSeconds +
      connector.secondarySharedSeconds

  // Brief road crossings remain possible. What is hard-rejected is a
  // sustained exposure on a primary road without a dedicated/separated
  // bicycle facility. This is a product safety ceiling, not an optimizer
  // weight: the user should not have to trade several continuous minutes in
  // primary-road traffic for a prettier climb sequence.
  val MaxLowProtectionPrimaryStreakSeconds = 120.0

  def connectorRoadForbidden(connector: Connector): Boolean =
    connector.hasMotorwayTrunk ||
      connector.longestLowProtectionPrimarySeconds >= MaxLowProtectionPrimaryStreakSeconds

  // Transfer downhill geometry is descriptive / soft only.
  // Explicit trails/avoid/*.gpx defines forbidden technical transfer corridors.
  // Generic dirt/path downhill remains available and downhillHandlingSeconds is
  // retained only as a late deterministic tie-break / rider diagnostic.

  // Wall difficulty has two deliberately separate layers:
  //
  // 1) ABSOLUTE HARD REJECT. This is the only permanent wall threshold. A
  //    connector at/above this envelope is not offered at all.
  // 2) RIDER-FACING C1/C2/C3 CEILINGS. These are discovered later from the
  //    locally useful connector variants in THIS matrix. Similar climbs are
  //    grouped by normalized severity, and the lightest ceiling is anchored
  //    at the first group that can still connect every mandatory trail.
  //
  // This fixes the previous semantic mistake where C1/C2/C3 themselves were
  // frozen global constants. The user-facing tiers now exist because they
  // actually gate useful transfers in the current ride.
  val ForbiddenWallGrade30Pct = 27.0
  val ForbiddenWallGrade100Pct = 20.0
  val ForbiddenWallMinAbove180Seconds =
    PowerPolicy.SafetyWallMinStreakSeconds

  val MaxOfferedWallClass = 3

  // Similarity tolerance only. This is NOT a rider difficulty boundary.
  // Severity is normalized to the permanent FORBIDDEN envelope, so 0.04 means
  // four percentage-points of that envelope. Groups are width-limited from
  // their first member, preventing chain-merging across a long dense range.
  val WallSeverityMergeEps = 0.04

  def connectorForbidden(connector: Connector): Boolean =
    connector.maxGrade30Pct >= ForbiddenWallGrade30Pct ||
      connector.maxGrade100Pct >= ForbiddenWallGrade100Pct ||
      connectorLongestPowerStreakSeconds(
        connector,
        PowerPolicy.SafetyWallPowerW
      ) >= ForbiddenWallMinAbove180Seconds

  def finiteNonNegative(x: Double): Double =
    if Format.finite(x) then math.max(0.0, x)
    else 0.0

  // One scalar is needed only to ORDER wall severity and group similar useful
  // connectors. Each physical signal is normalized by the same absolute hard
  // envelope; the hardest normalized dimension wins. Thus a short steep wall
  // and a longer sustained wall can land in the same rider-facing class.
  def connectorWallSeverity(connector: Connector): Double =
    Vector(
      finiteNonNegative(connector.maxGrade30Pct) / ForbiddenWallGrade30Pct,
      finiteNonNegative(connector.maxGrade100Pct) / ForbiddenWallGrade100Pct,
      finiteNonNegative(
        connectorLongestPowerStreakSeconds(
          connector,
          PowerPolicy.SafetyWallPowerW
        )
      ) / ForbiddenWallMinAbove180Seconds
    ).max

  def rawConnectorVariants(connector: Connector): Vector[Connector] =
    if connector.searchConnectorVariants.nonEmpty then connector.searchConnectorVariants
    else Vector(connector)

  // Rider-facing dynamic wall tiers are discovered after the locally useful
  // connector caches are built. Keeping discovery after local Pareto pruning
  // is intentional: dominated route-search noise must not create fake wall
  // classes.


  // ---------------------------------------------------------------------
  // CURRENT EXACT PRODUCT ORDERING CONTRACT
  //
  // Hard ordering rule: at least one non-demanding mandatory trail before the
  // first demanding technical descent. A second warm-up and avoiding demanding
  // adjacency are rider-quality preferences. No recovery-after-demanding block
  // and no forbidden full order are
  // part of the product model.
  // ---------------------------------------------------------------------

  require(trailCount > 0, "No mandatory trails available for route solver.")

  def trailBit(trailIndex: Int): Int =
    1 << trailIndex

  def isSteepTwistyTrail(trailIndex: Int): Boolean =
    effectiveSteepTwistyIndices.contains(trailIndex)

  // Product hard ordering contract:
  // at least ONE non-demanding mandatory trail must be ridden before the first
  // demanding technical descent. Demanding adjacency and demanding-last are
  // not hard-forbidden; they are rider-quality preferences.
  def trailPositionAllowed(trailIndex: Int, oneBasedPosition: Int): Boolean =
    !isSteepTwistyTrail(trailIndex) ||
      oneBasedPosition >= 2

  def trailTransitionAllowed(previousTrailIndex: Int, nextTrailIndex: Int, nextOneBasedPosition: Int): Boolean =
    trailPositionAllowed(nextTrailIndex, nextOneBasedPosition)

  def appendConnectorAscent(shape: ClimbShape, connector: Connector, zeroBasedTransferIndex: Int): ClimbShape =
    // endpoint->trail1 is transfer 0; trail1->trail2 is transfer 1.
    // Their ascent is deliberately irrelevant to fatigue shaping.
    if zeroBasedTransferIndex < 2 then shape
    else appendClimbShape(shape, connector.ascentM)

  val stateCount = 1 << trailCount

  val fullMask = stateCount - 1

  // Raw labeled variants are retained only because production real-ride
  // evidence is matched/applied before the corrected safety graph is built.
  case class LabeledUsefulConnector(label: String, connector: Connector)

  val labeledRawConnectorVariants =
    (
      trails.indices.flatMap { i =>
        matrix.startToTrail(i)
          .toVector
          .flatMap(rawConnectorVariants)
          .map { connector =>
            LabeledUsefulConnector(
              s"${endpointName(connector.route.from)} -> ${trails(i).name}",
              connector
            )
          }
      } ++
        trails.indices.flatMap { i =>
          trails.indices.flatMap { j =>
            if i == j then
              Vector.empty[
                LabeledUsefulConnector
              ]
            else
              matrix.between(i)(j)
                .toVector
                .flatMap(rawConnectorVariants)
                .map { connector =>
                  LabeledUsefulConnector(
                    s"${trails(i).name} -> ${trails(j).name}",
                    connector
                  )
                }
          }
        } ++
        trails.indices.flatMap { i =>
          matrix.trailToFinish(i)
            .toVector
            .flatMap(rawConnectorVariants)
            .map { connector =>
              LabeledUsefulConnector(
                s"${trails(i).name} -> ${endpointName(connector.route.to)}",
                connector
              )
            }
        }
    ).toVector

  case class WallSeverityGroup(
      ordinal: Int,
      minSeverity: Double,
      maxSeverity: Double,
      members: Vector[LabeledUsefulConnector],
      fullRouteFeasible: Boolean,
      cumulativeUsefulVariants: Int,
      cumulativeReachablePairs: Int,
      newlyReachablePairs: Int
  )

  case class RiddenTrailOccurrence(trailIndex: Int, startSampleIndex: Int, endSampleIndex: Int)

  case class RiddenTransferSegment(label: String, samples: Vector[RealRideSample])

  case class RiddenProfile(points: Vector[Point], coveragePct: Double, p90LateralM: Double, removedDriftMPerKm: Double)

  def percentile(values: Vector[Double], p: Double): Double =
    if values.isEmpty then
      Double.NaN
    else
      val sorted = values.sorted
      val idx =
        math.max(
          0,
          math.min(
            sorted.size - 1,
            math.round(
              (sorted.size - 1) *
                p
            ).toInt
          )
        )
      sorted(idx)

  def median(values: Vector[Double]): Double =
    percentile(values, 0.5)

  def robustLinearFit(samples: Vector[(Double, Double)], minimumPairSeparationM: Double = 80.0): (Double, Double) =
    if samples.size < 2 then
      (0.0, samples.headOption.map(_._2).getOrElse(0.0))
    else
      val slopes = Vector.newBuilder[Double]
      var i = 0
      while i + 1 < samples.size do
        val (
          x0,
          y0
        ) =
          samples(i)
        var j = i + 1
        while j < samples.size do
          val (
            x1,
            y1
          ) =
            samples(j)
          val dx = x1 -
              x0
          if dx >= minimumPairSeparationM
          then
            slopes +=
              (
                y1 -
                  y0
              ) / dx
          j += 1
        i += 1
      val slopeValues = slopes.result()
      val slope =
        if slopeValues.nonEmpty then median(slopeValues)
        else 0.0
      val intercept =
        median(
          samples.map {
            case (
                  x,
                  y
                ) =>
              y -
                slope * x
          }
        )
      (slope, intercept)

  def smoothResidualSeries(values: Vector[Double]): Vector[Double] =
    if values.size < 3 then
      values
    else
      val medianed = values.indices.map { i =>
          val from = math.max(0, i - 2)
          val until = math.min(values.size, i + 3)
          median(values.slice(from, until))
        }.toVector
      val weights = Vector(1.0, 2.0, 3.0, 2.0, 1.0)
      medianed.indices.map { i =>
        var weighted = 0.0
        var totalWeight = 0.0
        var k = -2
        while k <= 2 do
          val j = math.max(0, math.min(medianed.size - 1, i + k))
          val w = weights(k + 2)
          weighted +=
            medianed(j) * w
          totalWeight +=
            w
          k += 1
        weighted /
          totalWeight
      }.toVector

  def localWindowGradeSeries(points: Vector[Point], windowM: Double): Vector[(Double, Double)] =
    Geometry.exactSustainedGradeWindows(points, windowM)

  def commonWallGradeEvidence(first: Vector[(Double, Double)], second: Vector[(Double, Double)]): Option[(Double, Double, Double)] =
    if first.isEmpty ||
        second.isEmpty
    then
      None
    else
      val secondSamples = second
      val candidates =
        first.flatMap {
          case (
                distance,
                grade1
              ) =>
            interpolateProfile(
              secondSamples,
              distance
            ).map { grade2 =>
              (distance, grade1, grade2, math.min(grade1, grade2))
            }
        }
      if candidates.isEmpty then
        None
      else
        val best = candidates.maxBy(_._4)
        Some(
          (
            best._1,
            best._4,
            math.abs(
              best._2 -
                best._3
            )
          )
        )

  def riddenTrailOccurrence(ride: RealRide, trailIndex: Int): Option[RiddenTrailOccurrence] =
    val trail = sourceTrails(trailIndex)
    val denseTrail = Geometry.resample(trail.points, 8.0)
    val trailLength = Geometry.pathLengthMeters(denseTrail)
    if denseTrail.size < 2 ||
        trailLength < 30.0
    then
      None
    else
      val projected = ride.samples.map { sample =>
          Geometry.projectToPolyline(
            sample.point,
            denseTrail
          ).getOrElse(Geometry.PolylineProjection(0.0, Double.PositiveInfinity))
        }
      val rideCum = Array.ofDim[Double](ride.samples.size)
      var i = 1
      while i < ride.samples.size do
        val previous = ride.samples(i - 1)
        val current = ride.samples(i)
        rideCum(i) =
          rideCum(i - 1) +
            (
              if previous.segmentIndex == current.segmentIndex
              then
                Geometry.distanceMeters(previous.point, current.point)
              else
                0.0
            )
        i += 1
      val startCandidates = projected.indices.filter { i =>
          projected(i).lateralM <= 18.0 &&
            projected(i).alongM <= 35.0
        }

      // Permit a real traversal to leave the catalog line near the end.
      // This is important for Abschluss: both recordings follow the catalog
      // for roughly 550/595 m and then diverge. Requiring the exact catalog
      // endpoint would wrongly erase a clearly ridden trail occurrence.
      val endCandidates = projected.indices.filter { i =>
          projected(i).lateralM <= 18.0 &&
            projected(i).alongM >= trailLength * 0.82
        }
      var best = Option.empty[
          (Int, Int, Double)
        ]
      startCandidates
        .take(60)
        .foreach { first =>
          endCandidates
            .iterator
            .filter { last =>
                last > first &&
                  ride.samples(last).segmentIndex == ride.samples(first).segmentIndex
            }
            .take(80)
            .foreach { last =>
              val expectedProgress = projected(last).alongM
              val riddenSpan = rideCum(last) -
                  rideCum(first)
              val spanOk =
                riddenSpan >=
                  math.max(
                    20.0,
                    expectedProgress * 0.55
                  ) &&
                  riddenSpan <= expectedProgress * 1.80 +
                      100.0
              if spanOk then
                val near =
                  projected.slice(
                    first,
                    last + 1
                  ).filter(_.lateralM <= 18.0)
                if near.size >= 5 then
                  val alongs = near.map(_.alongM)
                  val coverage =
                    (
                      alongs.max -
                        alongs.min
                    ) /
                      trailLength
                  val nearFraction = near.size.toDouble /
                      math.max(
                        1,
                        last - first + 1
                      ).toDouble
                  val p90Gap =
                    percentile(
                      near.map(
                        _.lateralM
                      ),
                      0.90
                    )
                  if coverage >= 0.80 &&
                      nearFraction >= 0.45
                  then
                    val score =
                      math.abs(
                        riddenSpan -
                          expectedProgress
                      ) +
                        p90Gap * 5.0 +
                        (
                          1.0 -
                            nearFraction
                        ) * 100.0 +
                        (
                          1.0 -
                            coverage
                        ) * 20.0
                    best match
                      case None =>
                        best = Some((first, last, score))
                      case Some(
                            (_, _, oldScore)
                          ) =>
                        if score < oldScore then
                          best = Some((first, last, score))
            }
        }
      best.map {
        case (
              first,
              last,
              _
            ) =>
          RiddenTrailOccurrence(trailIndex, first, last)
      }

  def riddenTransferSegments(ride: RealRide): Vector[RiddenTransferSegment] =
    val occurrences = trails.indices
        .flatMap { i =>
          riddenTrailOccurrence(ride, i)
        }
        .sortBy(_.startSampleIndex)
        .foldLeft(
          Vector.empty[
            RiddenTrailOccurrence
          ]
        ) { (acc, current) =>
          if acc.lastOption.exists(
            previous =>
              current.startSampleIndex <= previous.endSampleIndex
          ) then
            acc
          else
            acc :+
              current
        }
    if occurrences.size < 2 then Vector.empty
    else occurrences
        .sliding(2)
        .flatMap {
          case Vector(from, to)
              if to.startSampleIndex >
                from.endSampleIndex + 2 &&
                ride.samples(from.endSampleIndex).segmentIndex == ride.samples(to.startSampleIndex).segmentIndex =>
            val samples = ride.samples.slice(from.endSampleIndex, to.startSampleIndex + 1)
            Some(
              RiddenTransferSegment(
                s"${trails(from.trailIndex).name} -> ${trails(to.trailIndex).name}",
                samples
              )
            )
          case _ =>
            None
        }
        .toVector

  def movingRideSamples(samples: Vector[RealRideSample]): Vector[RealRideSample] =
    if samples.size < 3 then samples
    else samples.indices.flatMap { i =>
        if i <= 0 ||
            i + 1 >= samples.size
        then
          None
        else
          val current = samples(i)
          val previous = samples(i - 1)
          val next = samples(i + 1)
          val dtPrev = current.epochSeconds -
              previous.epochSeconds
          val dtNext = next.epochSeconds -
              current.epochSeconds
          val progressSpeed =
            if dtPrev > 0.0 &&
                dtPrev <= 20.0
            then
              Geometry.distanceMeters(
                previous.point,
                current.point
              ) / dtPrev
            else
              0.0
          val sensorMoving = current.speedMps.exists(_ >= 0.45)
          val progressMoving =
            progressSpeed >= 0.30
          val sameContinuity =
            previous.segmentIndex == current.segmentIndex &&
              current.segmentIndex == next.segmentIndex
          val usable = sameContinuity &&
              current.point.ele.isDefined &&
              dtPrev > 0.0 &&
              dtPrev <= 20.0 &&
              dtNext > 0.0 &&
              dtNext <= 20.0 &&
              (
                sensorMoving ||
                  progressMoving
              )
          if usable then Some(current)
          else None
      }.toVector

  def interpolateProfile(samples: Vector[(Double, Double)], distanceM: Double): Option[Double] =
    if samples.isEmpty then
      None
    else if distanceM <= samples.head._1
    then
      Some(samples.head._2)
    else if distanceM >= samples.last._1
    then
      Some(samples.last._2)
    else
      var i = 0
      while i + 1 <
          samples.size
      do
        val (
          d0,
          z0
        ) =
          samples(i)
        val (
          d1,
          z1
        ) =
          samples(i + 1)
        if distanceM >= d0 &&
            distanceM <= d1
        then
          val span = d1 - d0
          if span <= 1e-9 then
            return Some(z1)
          val t =
            (
              distanceM -
                d0
            ) / span
          return Some(
            z0 +
              (
                z1 -
                  z0
              ) * t
          )
        i += 1
      None

  def riddenProfileOnReference(segment: RiddenTransferSegment, reference: Vector[Point]): Option[RiddenProfile] =
    val referenceLength = Geometry.pathLengthMeters(reference)
    if referenceLength < 80.0 then
      None
    else
      val projected =
        movingRideSamples(
          segment.samples
        ).flatMap { sample =>
          Geometry.projectToPolyline(
            sample.point,
            reference
          ).flatMap { projection =>
            sample.point.ele.map { elevation =>
              (projection.alongM, projection.lateralM, elevation)
            }
          }
        }.filter {
          case (
                _,
                lateral,
                _
              ) =>
            lateral <= 22.0
        }.sortBy(_._1)
      if projected.size < 10 then
        None
      else
        val p90Lateral = percentile(projected.map(_._2), 0.90)
        val minAlong = projected.head._1
        val maxAlong = projected.last._1
        val coverage =
          (
            maxAlong -
              minAlong
          ) /
            referenceLength
        val endpointCoverageOk = minAlong <= 40.0 &&
            maxAlong >= referenceLength -
                40.0

        // The second phone in the real ride is horizontally offset by roughly
        // 10-15 m on some long transfers. That is still useful evidence when
        // both recordings follow the same corridor. A 12 m p90 gate was too
        // strict and discarded two known-common transfers in V4-12A.
        if coverage < 0.75 ||
            !endpointCoverageOk ||
            p90Lateral > 18.0
        then
          None
        else
          val binSizeM = 10.0
          val binned = projected
              .groupBy {
                case (
                      along,
                      _,
                      _
                    ) =>
                  math.round(
                    along /
                      binSizeM
                  ).toInt
              }
              .toVector
              .sortBy(_._1)
              .map {
                case (
                      bin,
                      xs
                    ) =>
                  (
                    bin.toDouble *
                      binSizeM,
                    median(xs.map(_._3))
                  )
              }
          val referenceWithDistance = Geometry.pointsWithCumulativeDistance(Geometry.resample(reference, 10.0))
          val rawPhoneProfile =
            referenceWithDistance.flatMap {
              case (
                    distance,
                    point
                  ) =>
                interpolateProfile(
                  binned,
                  distance
                ).map { z =>
                  point.copy(
                    ele = Some(z)
                  )
                }
            }
          if rawPhoneProfile.size != referenceWithDistance.size
          then
            None
          else
            // Raw phone altitudes have large absolute offsets and slow drift.
            // Fit only an AFFINE phone-vs-Valhalla residual over long point
            // separations. Subtracting that low-order drift cannot invent or
            // erase a short 30/100 m wall; local residual shape remains.
            val deltaSamples =
              referenceWithDistance.zip(
                rawPhoneProfile
              ).flatMap {
                case (
                      (
                        distance,
                        referencePoint
                      ),
                      phonePoint
                    ) =>
                  for
                    referenceElevation <-
                      referencePoint.ele
                    phoneElevation <-
                      phonePoint.ele
                  yield
                    (
                      distance,
                      phoneElevation -
                        referenceElevation
                    )
              }
            if deltaSamples.size <
                5
            then
              None
            else
              val (
                driftSlope,
                driftIntercept
              ) =
                robustLinearFit(deltaSamples)
              val rawResiduals =
                referenceWithDistance.zip(
                  rawPhoneProfile
                ).map {
                  case (
                        (
                          distance,
                          referencePoint
                        ),
                        phonePoint
                      ) =>
                    (
                      referencePoint.ele,
                      phonePoint.ele
                    ) match
                      case (
                            Some(referenceElevation),
                            Some(phoneElevation)
                          ) =>
                        phoneElevation -
                          referenceElevation -
                          (
                            driftIntercept +
                              driftSlope *
                                distance
                          )
                      case _ =>
                        0.0
                }
              val smoothedResiduals = smoothResidualSeries(rawResiduals)
              val taperM = 35.0
              val corrected =
                referenceWithDistance.zip(
                  smoothedResiduals
                ).map {
                  case (
                        (
                          distance,
                          referencePoint
                        ),
                        residual
                      ) =>
                    val edgeTaper =
                      math.max(
                        0.0,
                        math.min(
                          1.0,
                          math.min(
                            distance /
                              taperM,
                            (
                              referenceLength -
                                distance
                            ) /
                              taperM
                          )
                        )
                      )
                    referencePoint.ele match
                      case Some(referenceElevation) =>
                        referencePoint.copy(
                          ele =
                            Some(
                              referenceElevation +
                                residual *
                                  edgeTaper
                            )
                        )
                      case None =>
                        referencePoint
                }
              Some(RiddenProfile(corrected, coverage * 100.0, p90Lateral, driftSlope * 1000.0))

  val riddenTransferSegmentsByRide = realRides.map { ride =>
      ride.path ->
        riddenTransferSegments(ride)
    }.toMap

  val riddenTransferLabels = riddenTransferSegmentsByRide.values
      .flatMap(_.map(_.label))
      .toSet
      .toVector
      .sorted

  case class RealRideWallEvidence(
      label: String,
      referenceGeometry: Vector[Point],
      common30: Option[(Double, Double, Double)],
      common100: Option[(Double, Double, Double)]
  )

  case class CorridorMatch(
      matched: Boolean,
      candidateToRideP90M: Double,
      rideToCandidateP90M: Double,
      lengthRatio: Double,
      startGapM: Double,
      endGapM: Double
  )

  def corridorMatch(candidate: Vector[Point], riddenReference: Vector[Point]): CorridorMatch =
    val candidateDense = Geometry.resample(candidate, 20.0)
    val riddenDense = Geometry.resample(riddenReference, 20.0)
    val candidateLength = Geometry.pathLengthMeters(candidateDense)
    val riddenLength = Geometry.pathLengthMeters(riddenDense)
    val ratio =
      if riddenLength > 1e-9 then
        candidateLength /
          riddenLength
      else
        Double.PositiveInfinity

    def p90DistanceTo(from: Vector[Point], to: Vector[Point]): Double =
      percentile(
        from.flatMap { point =>
          Geometry.projectToPolyline(
            point,
            to
          ).map(_.lateralM)
        },
        0.90
      )

    val c2r = p90DistanceTo(candidateDense, riddenDense)
    val r2c = p90DistanceTo(riddenDense, candidateDense)
    val startGap =
      for
        a <- candidateDense.headOption
        b <- riddenDense.headOption
      yield Geometry.distanceMeters(a, b)
    val endGap =
      for
        a <- candidateDense.lastOption
        b <- riddenDense.lastOption
      yield Geometry.distanceMeters(a, b)
    val startGapM = startGap.getOrElse(Double.PositiveInfinity)
    val endGapM = endGap.getOrElse(Double.PositiveInfinity)

    // The real recordings are evidence for the SAME ridden corridor only.
    // This prevents a wall observed on one direct transfer from contaminating
    // a deliberately different detour between the same pair of trails.
    val matched = candidateDense.size >= 2 &&
        riddenDense.size >= 2 &&
        ratio >= 0.75 &&
        ratio <= 1.30 &&
        c2r <= 18.0 &&
        r2c <= 18.0 &&
        startGapM <= 70.0 &&
        endGapM <= 70.0
    CorridorMatch(matched, c2r, r2c, ratio, startGapM, endGapM)

  case class LocalEvidenceCorridorMatch(matched: Boolean, maxRideToCandidateM: Double, p90RideToCandidateM: Double)

  // A global corridor match is not enough to transfer a LOCAL wall: a variant
  // may follow the ridden route for >90% of its length but bypass exactly the
  // steep 30/100 m section. Require every dense ridden-reference sample across
  // the accepted evidence window to stay inside the existing 18 m corridor
  // tolerance of the generated candidate.
  def localEvidenceCorridorMatch(
      candidate: Vector[Point],
      riddenReference: Vector[Point],
      centerM: Double,
      windowM: Double
  ): LocalEvidenceCorridorMatch =
    val candidateDense = Geometry.resample(candidate, 5.0)
    val referenceDense = Geometry.resample(riddenReference, 5.0)
    val startM = math.max(0.0, centerM - windowM / 2.0)
    val endM = centerM + windowM / 2.0
    val localReference =
      Geometry.pointsWithCumulativeDistance(
        referenceDense
      ).collect {
        case (distanceM, point)
            if distanceM >= startM - 1e-9 &&
              distanceM <= endM + 1e-9 =>
          point
      }
    val projections = localReference.flatMap { point =>
        Geometry.projectToPolyline(point, candidateDense)
      }
    val distances = projections.map(_.lateralM)

    // Local wall evidence is directional. Require substantial forward
    // progress through the same candidate window, but tolerate a few metres
    // of projection jitter on bends. Reversed traversal has a negative span.
    val directionSpanM =
      if projections.size >= 2 then
        projections.last.alongM -
          projections.head.alongM
      else
        Double.NegativeInfinity
    val directionConsistent = projections.size >= 2 &&
        directionSpanM >=
          math.max(
            10.0,
            windowM * 0.50
          ) &&
        projections.sliding(2).forall {
          case Vector(a, b) =>
            b.alongM >= a.alongM - 5.0
          case _ =>
            true
        }
    val maxDistance =
      if distances.nonEmpty then distances.max
      else Double.PositiveInfinity
    val p90Distance = percentile(distances, 0.90)
    LocalEvidenceCorridorMatch(
      matched =
        localReference.size >= 4 &&
          projections.size == localReference.size &&
          directionConsistent &&
          maxDistance <= 18.0,
      maxRideToCandidateM = maxDistance,
      p90RideToCandidateM = p90Distance
    )

  // Production evidence is intentionally stricter than the broad diagnostic
  // above. A wall floor is accepted only when BOTH phones show the uphill at
  // the same non-edge route location and agree reasonably on its grade.
  //
  // 30 m evidence:
  //   - window center >=45 m from either end
  //   - phones differ by <=5 grade points
  //   - conservative common grade >= Valhalla +4 points
  //
  // 100 m evidence:
  //   - window center >=60 m from either end
  //   - phones differ by <=4 grade points
  //   - conservative common grade >= Valhalla +3 points
  //
  // Power-only disagreement remains diagnostic and never changes production
  // wall class because the two longest power streaks may occur at different
  // locations.
  val productionRealRideWallEvidence = riddenTransferLabels.flatMap { label =>
      val segments = realRides.flatMap { ride =>
          riddenTransferSegmentsByRide
            .getOrElse(ride.path, Vector.empty)
            .find(_.label == label)
            .map(
              ride ->
                _
            )
        }
      if segments.size < 2 then
        None
      else
        val movingReference =
          movingRideSamples(
            segments.head._2.samples
          ).map(_.point)
        if movingReference.size < 5 then
          None
        else
          val referenceGeometry =
            try
              val dense = Geometry.resample(movingReference, 10.0)
              valhalla.withElevation(dense)
            catch
              case _: Exception =>
                Vector.empty[Point]
          if referenceGeometry.size < 2 then
            None
          else
            val profiles =
              segments.flatMap {
                case (
                      _,
                      segment
                    ) =>
                  riddenProfileOnReference(segment, referenceGeometry)
              }
            if profiles.size < 2 then
              None
            else
              val first = profiles(0)
              val second = profiles(1)
              val referenceLength = Geometry.pathLengthMeters(referenceGeometry)
              val common30 =
                commonWallGradeEvidence(
                  localWindowGradeSeries(
                    first.points,
                    30.0
                  ),
                  localWindowGradeSeries(second.points, 30.0)
                )
              val common100 =
                commonWallGradeEvidence(
                  localWindowGradeSeries(
                    first.points,
                    100.0
                  ),
                  localWindowGradeSeries(second.points, 100.0)
                )
              val valhallaStats = Geometry.profileStats(referenceGeometry)
              val accepted30 =
                common30.filter {
                  case (
                        distance,
                        grade,
                        phoneDifference
                      ) =>
                    distance >= 45.0 &&
                      distance <= referenceLength - 45.0 &&
                      phoneDifference <= 5.0 &&
                      grade >= valhallaStats.maxGrade30Pct + 4.0
                }
              val accepted100 =
                common100.filter {
                  case (
                        distance,
                        grade,
                        phoneDifference
                      ) =>
                    distance >= 60.0 &&
                      distance <= referenceLength - 60.0 &&
                      phoneDifference <= 4.0 &&
                      grade >= valhallaStats.maxGrade100Pct + 3.0
                }
              val severityFloor =
                Vector(
                  accepted30
                    .map(_._2)
                    .map { grade =>
                        grade /
                          ForbiddenWallGrade30Pct
                    }
                    .getOrElse(0.0),
                  accepted100
                    .map(_._2)
                    .map { grade =>
                        grade /
                          ForbiddenWallGrade100Pct
                    }
                    .getOrElse(0.0)
                ).max
              val valhallaSeverity =
                Vector(
                  math.max(
                    0.0,
                    valhallaStats.maxGrade30Pct
                  ) /
                    ForbiddenWallGrade30Pct,
                  math.max(
                    0.0,
                    valhallaStats.maxGrade100Pct
                  ) /
                    ForbiddenWallGrade100Pct
                ).max
              if severityFloor >
                  valhallaSeverity + 0.06
              then
                Some(RealRideWallEvidence(label, referenceGeometry, accepted30, accepted100))
              else
                None
    }

  val realRideWallSeverityFloorByConnector = new java.util.IdentityHashMap[
      Connector,
      java.lang.Double
    ]()
  productionRealRideWallEvidence.foreach { evidence =>
    labeledRawConnectorVariants
      .filter(_.label == evidence.label)
      .foreach { item =>
        val connector = item.connector
        val matchResult = corridorMatch(connector.route.points, evidence.referenceGeometry)
        val local30 =
          evidence.common30.map {
            case (distance, grade, _) =>
              (
                localEvidenceCorridorMatch(
                  connector.route.points,
                  evidence.referenceGeometry,
                  distance,
                  30.0
                ),
                grade / ForbiddenWallGrade30Pct
              )
          }
        val local100 =
          evidence.common100.map {
            case (distance, grade, _) =>
              (
                localEvidenceCorridorMatch(
                  connector.route.points,
                  evidence.referenceGeometry,
                  distance,
                  100.0
                ),
                grade / ForbiddenWallGrade100Pct
              )
          }
        val variantEvidenceFloor =
          if matchResult.matched then
            Vector(
              local30.collect {
                case (local, floor) if local.matched => floor
              }.getOrElse(0.0),
              local100.collect {
                case (local, floor) if local.matched => floor
              }.getOrElse(0.0)
            ).max
          else
            0.0
        if variantEvidenceFloor > 0.0 then
          val previous =
            Option(
              realRideWallSeverityFloorByConnector.get(connector)
            ).map(_.doubleValue()).getOrElse(0.0)
          realRideWallSeverityFloorByConnector.put(
            connector,
            java.lang.Double.valueOf(math.max(previous, variantEvidenceFloor))
          )
      }
  }

  def realRideWallSeverityFloor(connector: Connector): Double =
    Option(
      realRideWallSeverityFloorByConnector.get(connector)
    ).map(
      _.doubleValue()
    ).getOrElse(0.0)

  def effectiveConnectorWallSeverity(connector: Connector): Double =
    math.max(
      connectorWallSeverity(
        connector
      ),
      realRideWallSeverityFloor(connector)
    )
  println()
  // ---------------------------------------------------------------------
  // BUILD THE LOCALLY USEFUL MATRIX AFTER REAL-RIDE WALL EVIDENCE AND SAFETY FILTERS.
  //
  // V4-14 applied real evidence only at final wall-class assignment. That was
  // safe for the selected route, but it was one stage too late:
  //   - a dual-ride-forbidden connector could still participate in local
  //     pruning and in dynamic C1/C2/C3 breakpoint discovery;
  //   - if that promoted connector had previously dominated another route
  //     variant, the safer alternative might already have been discarded.
  //
  // Re-run raw-variant filtering + local Pareto pruning with EFFECTIVE wall
  // severity, then re-run the exact reachability-based wall discovery. All
  // downstream solver caches use this corrected matrix.
  // ---------------------------------------------------------------------

  def effectiveConnectorForbidden(connector: Connector): Boolean =
    connectorForbidden(
      connector
    ) ||
      connectorRoadForbidden(
        connector
      ) ||
      effectiveConnectorWallSeverity(
        connector
      ) >= 1.0 - 1e-9

  def sameDoubleBits(a: Double, b: Double): Boolean =
    java.lang.Double.doubleToLongBits(
      a
    ) == java.lang.Double.doubleToLongBits(b)

  def sameIntDoubleMapBits(a: Map[Int, Double], b: Map[Int, Double]): Boolean =
    a.keySet == b.keySet &&
      a.forall { case (key, value) =>
        b.get(key).exists(other => sameDoubleBits(value, other))
      }

  def samePointBits(a: Point, b: Point): Boolean =
    sameDoubleBits(
      a.lat,
      b.lat
    ) &&
      sameDoubleBits(
        a.lon,
        b.lon
      ) &&
      (
        (
          a.ele,
          b.ele
        ) match
          case (
                Some(x),
                Some(y)
              ) =>
            sameDoubleBits(x, y)
          case (
                None,
                None
              ) =>
            true
          case _ =>
            false
      )

  def samePointSequenceBits(a: Vector[Point], b: Vector[Point]): Boolean =
    a.size == b.size &&
      a.zip(
        b
      ).forall {
        case (
              x,
              y
            ) =>
          samePointBits(x, y)
      }

  // Exact solver-semantic duplicate identity.
  //
  // Search-profile parameters are GENERATION inputs, not extra votes in
  // rider-facing wall-tier discovery. Two candidates collapse only when:
  //   - both routed geometries are bit-identical;
  //   - every downstream safety/objective quantity is bit-identical;
  //   - their POST-real-ride effective wall severity is bit-identical.
  //
  // We intentionally ignore routingSpeedKph/routingUseHills/routingUseRoads.
  // They explain how an identical connector was discovered, but must not
  // multiply its weight in terrain discovery or in the exact DP frontier.
  def sameCorrectedSolverSemanticConnector(a: Connector, b: Connector): Boolean =
    val sameGeometry =
      samePointSequenceBits(
        a.route.points,
        b.route.points
      ) &&
        samePointSequenceBits(a.route.rawValhallaPoints, b.route.rawValhallaPoints)
    val sameScalars =
      Vector(
        (a.ascentM, b.ascentM),
        (a.descentM, b.descentM),
        (a.lateAscentM, b.lateAscentM),
        (a.physicsSeconds, b.physicsSeconds),
        (a.fatiguePenaltySeconds, b.fatiguePenaltySeconds),
        (
          a.transferQualityPenaltySeconds,
          b.transferQualityPenaltySeconds
        ),
        (a.candidateComfortPenaltySeconds, b.candidateComfortPenaltySeconds),
        (a.candidateComfortSpikeLoadSeconds, b.candidateComfortSpikeLoadSeconds),
        (a.maxRiderPowerW, b.maxRiderPowerW),
        (a.maxGrade30Pct, b.maxGrade30Pct),
        (a.maxGrade100Pct, b.maxGrade100Pct),
        (a.majorRoadSeconds, b.majorRoadSeconds),
        (a.motorwayTrunkSeconds, b.motorwayTrunkSeconds),
        (a.primaryNoCycleSeconds, b.primaryNoCycleSeconds),
        (a.primarySharedSeconds, b.primarySharedSeconds),
        (a.primaryWithCycleSeconds, b.primaryWithCycleSeconds),
        (a.secondaryNoCycleSeconds, b.secondaryNoCycleSeconds),
        (a.secondarySharedSeconds, b.secondarySharedSeconds),
        (
          a.longestLowProtectionPrimarySeconds,
          b.longestLowProtectionPrimarySeconds
        ),
        (a.unpavedSeconds, b.unpavedSeconds),
        (a.downhillHandlingSeconds, b.downhillHandlingSeconds),
        (
          a.longestTechnicalDownhillRunM,
          b.longestTechnicalDownhillRunM
        ),
        (
          a.maxTechnicalDownhillGrade30Pct,
          b.maxTechnicalDownhillGrade30Pct
        ),
        (
          a.maxTechnicalDownhillGrade100Pct,
          b.maxTechnicalDownhillGrade100Pct
        ),
        (
          a.maxTechnicalPathDownhillGrade30Pct,
          b.maxTechnicalPathDownhillGrade30Pct
        ),
        (a.effectiveCrr, b.effectiveCrr),
        (a.pathFraction, b.pathFraction),
        (a.route.lengthKm, b.route.lengthKm),
        (
          effectiveConnectorWallSeverity(
            a
          ),
          effectiveConnectorWallSeverity(b)
        )
      ).forall {
        case (
              x,
              y
            ) =>
          sameDoubleBits(x, y)
      }
    sameGeometry &&
      sameScalars &&
      sameIntDoubleMapBits(a.powerAboveSecondsByThreshold, b.powerAboveSecondsByThreshold) &&
      sameIntDoubleMapBits(
        a.longestPowerStreakSecondsByThreshold,
        b.longestPowerStreakSecondsByThreshold
      ) &&
      a.hasMotorwayTrunk == b.hasMotorwayTrunk

  def collapseCorrectedSolverSemanticDuplicates(connectors: Vector[Connector]): Vector[Connector] =
    connectors.foldLeft(
      Vector.empty[Connector]
    ) {
      case (
            kept,
            candidate
          ) =>
        if kept.exists(
            existing =>
              sameCorrectedSolverSemanticConnector(existing, candidate)
          )
        then
          kept
        else
          kept :+
            candidate
    }

  def correctedConnectorDominates(a: Connector, b: Connector): Boolean =
    val wallA = effectiveConnectorWallSeverity(a)
    val wallB = effectiveConnectorWallSeverity(b)
    val sameAscent =
      java.lang.Double.compare(
        a.ascentM,
        b.ascentM
      ) == 0
    val notWorse = sameAscent &&
        wallA <= wallB &&
        a.physicsSeconds <= b.physicsSeconds &&
        connectorRoadStress(a) <= connectorRoadStress(b) &&
        a.downhillHandlingSeconds <= b.downhillHandlingSeconds &&
        a.transferQualityPenaltySeconds <= b.transferQualityPenaltySeconds &&
        a.candidateComfortPenaltySeconds <= b.candidateComfortPenaltySeconds &&
        connectorCandidateLongestLow(a) <= connectorCandidateLongestLow(b) &&
        connectorCandidateLongestHigh(a) <= connectorCandidateLongestHigh(b) &&
        a.candidateComfortSpikeLoadSeconds <= b.candidateComfortSpikeLoadSeconds &&
        a.fatiguePenaltySeconds <= b.fatiguePenaltySeconds
    val strictlyBetter = wallA < wallB ||
        a.physicsSeconds < b.physicsSeconds ||
        connectorRoadStress(a) <
          connectorRoadStress(b) ||
        a.downhillHandlingSeconds <
          b.downhillHandlingSeconds ||
        a.transferQualityPenaltySeconds <
          b.transferQualityPenaltySeconds ||
        a.candidateComfortPenaltySeconds <
          b.candidateComfortPenaltySeconds ||
        connectorCandidateLongestLow(a) < connectorCandidateLongestLow(b) ||
        connectorCandidateLongestHigh(a) < connectorCandidateLongestHigh(b) ||
        a.candidateComfortSpikeLoadSeconds <
          b.candidateComfortSpikeLoadSeconds ||
        a.fatiguePenaltySeconds <
          b.fatiguePenaltySeconds
    notWorse &&
      strictlyBetter

  def correctedConnectorVariants(connector: Connector): Vector[Connector] =
    val offered =
      rawConnectorVariants(
        connector
      ).filterNot(effectiveConnectorForbidden)
    val distinct = collapseCorrectedSolverSemanticDuplicates(offered)
    distinct.filterNot { candidate =>
      distinct.exists { other =>
        (other ne candidate) &&
          correctedConnectorDominates(other, candidate)
      }
    }

  val correctedStartVariantCache =
    Vector.tabulate(
      trails.size
    ) { i =>
      matrix.startToTrail(i)
        .map(correctedConnectorVariants)
        .getOrElse(Vector.empty)
    }

  val correctedBetweenVariantCache =
    Vector.tabulate(
      trails.size
    ) { i =>
      Vector.tabulate(
        trails.size
      ) { j =>
        if i == j then Vector.empty[Connector]
        else matrix.between(i)(j)
            .map(correctedConnectorVariants)
            .getOrElse(Vector.empty)
      }
    }

  val correctedFinishVariantCache =
    Vector.tabulate(
      trails.size
    ) { i =>
      matrix.trailToFinish(i)
        .map(correctedConnectorVariants)
        .getOrElse(Vector.empty)
    }

  val correctedUsefulConnectorVariants =
    (
      trails.indices.flatMap { i =>
        correctedStartVariantCache(i).map { connector =>
          LabeledUsefulConnector(
            s"START -> ${trails(i).name}",
            connector
          )
        }
      } ++
        trails.indices.flatMap { i =>
          trails.indices.flatMap { j =>
            if i == j then
              Vector.empty[
                LabeledUsefulConnector
              ]
            else
              correctedBetweenVariantCache(i)(j).map { connector =>
                LabeledUsefulConnector(
                  s"${trails(i).name} -> ${trails(j).name}",
                  connector
                )
              }
          }
        } ++
        trails.indices.flatMap { i =>
          correctedFinishVariantCache(i).map { connector =>
            LabeledUsefulConnector(
              s"${trails(i).name} -> ENDPOINT",
              connector
            )
          }
        }
    ).toVector


  require(
    correctedUsefulConnectorVariants.nonEmpty,
    "No locally useful connector variants survived the corrected road/wall safety matrix."
  )

  def correctedAllowedAtWallSeverity(connector: Connector, maxSeverity: Double): Boolean =
    !effectiveConnectorForbidden(
      connector
    ) &&
      effectiveConnectorWallSeverity(
        connector
      ) <= maxSeverity + 1e-9

  def correctedFullMandatoryRouteFeasibleAtSeverity(maxSeverity: Double): Boolean =
    def key(mask: Int, last: Int): Long =
      (mask.toLong << 32) | (last.toLong & 0xffffffffL)

    def keyMask(value: Long): Int =
      (value >>> 32).toInt

    def keyLast(value: Long): Int =
      value.toInt

    var current = mutable.HashSet.empty[Long]
    trails.indices.foreach { i =>
      if trailPositionAllowed(
            i,
            1
          ) &&
          correctedStartVariantCache(i).exists(
            connector =>
              correctedAllowedAtWallSeverity(connector, maxSeverity)
          )
      then
        current +=
          key(trailBit(i), i)
    }
    var layer = 1
    while layer < trailCount &&
        current.nonEmpty
    do
      val nextStates = mutable.HashSet.empty[Long]
      current.foreach { packed =>
        val mask = keyMask(packed)
        val last = keyLast(packed)
        trails.indices.foreach { next =>
          if (mask & trailBit(next)) == 0 &&
              trailTransitionAllowed(
                last,
                next,
                layer + 1
              ) &&
              correctedBetweenVariantCache(last)(next).exists(
                connector =>
                  correctedAllowedAtWallSeverity(connector, maxSeverity)
              )
          then
            nextStates +=
              key(mask | trailBit(next), next)
        }
      }
      current = nextStates
      layer +=
        1
    current.exists { packed =>
      val mask = keyMask(packed)
      val last = keyLast(packed)
      mask == fullMask &&
        correctedFinishVariantCache(last).exists(
          connector =>
            correctedAllowedAtWallSeverity(connector, maxSeverity)
        )
    }

  val correctedSeveritySorted =
    correctedUsefulConnectorVariants.sortBy(
      item =>
        effectiveConnectorWallSeverity(item.connector)
    )

  val correctedSeverityBuckets = mutable.ArrayBuffer.empty[
      mutable.ArrayBuffer[
        LabeledUsefulConnector
      ]
    ]
  correctedSeveritySorted.foreach { item =>
    val severity = effectiveConnectorWallSeverity(item.connector)
    correctedSeverityBuckets.lastOption match
      case Some(bucket) =>
        val bucketMin = effectiveConnectorWallSeverity(bucket.head.connector)
        if severity - bucketMin <= WallSeverityMergeEps + 1e-9
        then
          bucket +=
            item
        else
          correctedSeverityBuckets +=
            mutable.ArrayBuffer(item)
      case None =>
        correctedSeverityBuckets +=
          mutable.ArrayBuffer(item)
  }

  val correctedRawSeverityGroups =
    correctedSeverityBuckets.toVector.zipWithIndex.map {
      case (
            bucket,
            idx
          ) =>
        val members = bucket.toVector
        val severities =
          members.map { item =>
              effectiveConnectorWallSeverity(item.connector)
          }
        (idx + 1, severities.min, severities.max, members)
    }

  val correctedWallSeverityGroups =
    correctedRawSeverityGroups.zipWithIndex.map {
      case (
            (
              ordinal,
              minSeverity,
              maxSeverity,
              members
            ),
            idx
          ) =>
        val cumulativeMembers =
          correctedUsefulConnectorVariants.filter { item =>
              effectiveConnectorWallSeverity(
                item.connector
              ) <= maxSeverity + 1e-9
          }
        val cumulativePairs =
          cumulativeMembers.map(
            _.label
          ).toSet
        val previousPairs =
          if idx == 0 then
            Set.empty[String]
          else
            val previousMax =
              correctedRawSeverityGroups(
                idx - 1
              )._3
            correctedUsefulConnectorVariants
              .filter { item =>
                  effectiveConnectorWallSeverity(
                    item.connector
                  ) <= previousMax + 1e-9
              }
              .map(_.label)
              .toSet
        WallSeverityGroup(
          ordinal = ordinal,
          minSeverity = minSeverity,
          maxSeverity = maxSeverity,
          members = members,
          fullRouteFeasible =
            correctedFullMandatoryRouteFeasibleAtSeverity(
              maxSeverity
            ),
          cumulativeUsefulVariants = cumulativeMembers.size,
          cumulativeReachablePairs = cumulativePairs.size,
          newlyReachablePairs =
            (
              cumulativePairs --
                previousPairs
            ).size
        )
    }

  val correctedFeasibleWallGroups = correctedWallSeverityGroups.filter(_.fullRouteFeasible)

  if correctedFeasibleWallGroups.isEmpty then
    val reason =
      "No complete all-mandatory-trails route exists after road safety, explicit avoid corridors and production-strength real-ride wall evidence."
    Console.err.println(s"Planner stopped cleanly: $reason")
    writeEarlyFailureReports(reason, phase = "post-evidence corrected wall reachability")
    return

  val correctedC1Group = correctedFeasibleWallGroups.head

  val correctedC3Group = correctedFeasibleWallGroups.last

  val correctedC2Group =
    val interior = correctedFeasibleWallGroups.filter { group =>
        group.ordinal != correctedC1Group.ordinal &&
          group.ordinal != correctedC3Group.ordinal
      }
    if interior.isEmpty then
      correctedC3Group
    else
      val targetUsefulVariants =
        (
          correctedC1Group.cumulativeUsefulVariants +
            correctedC3Group.cumulativeUsefulVariants
        ).toDouble /
          2.0
      interior.minBy { group =>
        (
          math.abs(
            group.cumulativeUsefulVariants.toDouble -
              targetUsefulVariants
          ),
          -group.newlyReachablePairs,
          group.ordinal
        )
      }

  val correctedSelectedWallGroups = Vector(correctedC1Group, correctedC2Group, correctedC3Group)

  val correctedWallProfileCeilings = correctedSelectedWallGroups.map(_.maxSeverity)

  def finalEffectiveConnectorWallClass(connector: Connector): Int =
    val severity = effectiveConnectorWallSeverity(connector)
    if effectiveConnectorForbidden(connector)
    then
      MaxOfferedWallClass + 1
    else
      correctedWallProfileCeilings.indexWhere(
        ceiling =>
          severity <= ceiling + 1e-9
      ) match
        case -1 =>
          MaxOfferedWallClass + 1
        case idx =>
          idx + 1

  case class RiderEndpointMode(startEndpoint: Int, finishEndpoint: Int):
    def text: String =
      s"${endpointNames(startEndpoint - 1)} -> ${endpointNames(finishEndpoint - 1)}"

  // Sparse exact-DP state: current mandatory mask + last mandatory trail.
  // Diversity-prefix bits belonged only to the removed forbidden-order solver.
  def packedStateKey(mask: Int, last: Int): Long =
    ((mask.toLong & 0xffffffffL) << 32) |
      (last.toLong & 0xffffffffL)

  def packedStateMask(key: Long): Int =
    (key >>> 32).toInt

  def packedStateLast(key: Long): Int =
    key.toInt

  // Lexicographic tie-break used only by exact diagnostic DP.
  def terrainOrderLexLess(a: Vector[Int], b: Vector[Int]): Boolean =
    val firstDifference = a.indices.find(i => a(i) != b(i))
    firstDifference match
      case Some(i) => a(i) < b(i)
      case None    => a.size < b.size

  val terrainFrontierEndpointModes = Vector(
    "LOOP" -> RiderEndpointMode(startEndpoint = 2, finishEndpoint = 2),
    "P2P" -> RiderEndpointMode(startEndpoint = 2, finishEndpoint = 1)
  )

  // ---------------------------------------------------------------------
  // EXACT TERRAIN FRONTIER
  //
  // Hard ordering contract at terrain-discovery time:
  //   - at least ONE non-demanding mandatory trail before the first demanding
  //     technical descent;
  //   - second warm-up is NOT hard;
  //   - demanding adjacency is NOT hard;
  //   - demanding trail may be last.
  //
  // Human-quality preferences are applied later inside the calibrated shared
  // +60 s budget. Terrain discovery itself remains exact transfer-fastest.
  // ---------------------------------------------------------------------

  def terrainPositionAllowed(trailIndex: Int, oneBasedPosition: Int): Boolean =
    !isSteepTwistyTrail(trailIndex) || oneBasedPosition > 1

  def terrainTransitionAllowed(
      previousTrailIndex: Int,
      nextTrailIndex: Int,
      nextOneBasedPosition: Int
  ): Boolean =
    terrainPositionAllowed(nextTrailIndex, nextOneBasedPosition)

  case class TerrainFrontierRoute(
      transferSeconds: Double,
      order: Vector[Int],
      connectors: Vector[Connector]
  ):
    def maxUsedSeverity: Double =
      connectors.map(effectiveConnectorWallSeverity).foldLeft(0.0)(math.max)

  case class TerrainFrontierSeries(
      label: String,
      endpointMode: RiderEndpointMode,
      rows: Vector[(Double, Option[TerrainFrontierRoute])]
  )

  def terrainFrontierStateBetter(candidate: TerrainFrontierRoute, existing: TerrainFrontierRoute): Boolean =
    candidate.transferSeconds < existing.transferSeconds - 1e-9 ||
      (
        math.abs(candidate.transferSeconds - existing.transferSeconds) <= 1e-9 &&
          terrainOrderLexLess(candidate.order, existing.order)
      )

  def bestTerrainConnector(candidates: Vector[Connector]): Option[Connector] =
    candidates.minByOption { connector =>
      (
        connector.physicsSeconds,
        effectiveConnectorWallSeverity(connector),
        connector.ascentM,
        connector.route.lengthKm,
        connector.candidateComfortPenaltySeconds
      )
    }

  def exactFastestTerrainAtSeverity(
      maxSeverity: Double,
      endpointMode: RiderEndpointMode
  ): Option[TerrainFrontierRoute] =
    def startConnector(trailIndex: Int): Option[Connector] =
      bestTerrainConnector(
        correctedStartVariantCache(trailIndex)
          .filter(connector => correctedAllowedAtWallSeverity(connector, maxSeverity))
          .filter(connector => endpointNumber(connector.route.from) == endpointMode.startEndpoint)
      )

    def betweenConnector(fromTrail: Int, toTrail: Int): Option[Connector] =
      bestTerrainConnector(
        correctedBetweenVariantCache(fromTrail)(toTrail)
          .filter(connector => correctedAllowedAtWallSeverity(connector, maxSeverity))
      )

    def finishConnector(trailIndex: Int): Option[Connector] =
      bestTerrainConnector(
        correctedFinishVariantCache(trailIndex)
          .filter(connector => correctedAllowedAtWallSeverity(connector, maxSeverity))
          .filter(connector => endpointNumber(connector.route.to) == endpointMode.finishEndpoint)
      )

    var current = mutable.LongMap.empty[TerrainFrontierRoute]
    trails.indices.foreach { i =>
      if terrainPositionAllowed(i, 1) then
        startConnector(i).foreach { connector =>
          current.update(
            packedStateKey(trailBit(i), i),
            TerrainFrontierRoute(
              transferSeconds = connector.physicsSeconds,
              order = Vector(i),
              connectors = Vector(connector)
            )
          )
        }
    }

    var layer = 1
    while layer < trailCount && current.nonEmpty do
      val nextLayer = mutable.LongMap.empty[TerrainFrontierRoute]
      current.foreach { case (packed, state) =>
        val mask = packedStateMask(packed)
        val last = packedStateLast(packed)
        var next = 0
        while next < trails.size do
          if (mask & trailBit(next)) == 0 &&
              terrainTransitionAllowed(last, next, layer + 1)
          then
            betweenConnector(last, next).foreach { connector =>
              val candidate = TerrainFrontierRoute(
                transferSeconds = state.transferSeconds + connector.physicsSeconds,
                order = state.order :+ next,
                connectors = state.connectors :+ connector
              )
              val key = packedStateKey(mask | trailBit(next), next)
              nextLayer.get(key) match
                case Some(existing) if !terrainFrontierStateBetter(candidate, existing) => ()
                case _ => nextLayer.update(key, candidate)
            }
          next += 1
      }
      current = nextLayer
      layer += 1

    var best = Option.empty[TerrainFrontierRoute]
    current.foreach { case (packed, prefix) =>
      if packedStateMask(packed) == fullMask then
        val last = packedStateLast(packed)
        finishConnector(last).foreach { connector =>
          val candidate = TerrainFrontierRoute(
            transferSeconds = prefix.transferSeconds + connector.physicsSeconds,
            order = prefix.order,
            connectors = prefix.connectors :+ connector
          )
          best match
            case Some(existing) if !terrainFrontierStateBetter(candidate, existing) => ()
            case _ => best = Some(candidate)
        }
    }
    best

  val exactWallBreakpointSeverities: Vector[Double] =
    correctedUsefulConnectorVariants
      .map(item => effectiveConnectorWallSeverity(item.connector))
      .filter(severity => Format.finite(severity) && severity < 1.0 - 1e-9)
      .distinct
      .sorted

  println("[3/4   0.0%] Solving exact terrain frontier and rider-quality routes.")
  val exactWallBreakpointSeries =
    terrainFrontierEndpointModes.map { case (modeLabel, endpointMode) =>
      TerrainFrontierSeries(
        label = s"$modeLabel / WARMUP1_HARD",
        endpointMode = endpointMode,
        rows = exactWallBreakpointSeverities.map { severity =>
          severity -> exactFastestTerrainAtSeverity(severity, endpointMode)
        }
      )
    }

  val timingTerrainFrontierDoneNs = System.nanoTime()
  println("[3/4  25.0%] Exact terrain frontier ready; deriving RAW-terrain product classes.")

  // ---------------------------------------------------------------------
  // MIGRATION REFERENCE + V5 ROUTE METRICS
  //
  // HumanQualityRoute is the shared exact route representation for candidate
  // comfort and the temporary +60 migration baseline. The baseline now uses
  // only V5 candidate-comfort resources; fixed legacy power thresholds are gone.
  // It remains only as a temporary depth/no-regression reference for the three
  // selected class/endpoint pairs and does not discover classes/endpoints.
  // ---------------------------------------------------------------------

  case class HumanQualityRoute(
      mask: Int,
      last: Int,
      transferSeconds: Double,
      order: Vector[Int],
      connectors: Vector[Connector],
      warmup2Penalty: Int,
      demandingAdjacencyCount: Int,
      climbShape: ClimbShape,
      roadStressSeconds: Double,
      candidateComfortSufferingSeconds: Double = 0.0,
      candidateLongestLowSeconds: Double = 0.0,
      candidateLongestHighSeconds: Double = 0.0,
      candidateSpikeLoadSeconds: Double = 0.0
  )

  case class ProductRideQuality(
      maxCountedAscentM: Double,
      upwardViolationM: Double,
      roughnessM: Double,
      totalCountedAscentM: Double,
      roadStressSeconds: Double,
      candidateComfortSufferingSeconds: Double,
      downhillHandlingSeconds: Double,
      pathPenaltySeconds: Double
  )

  // Temporary migration baseline for exactly the three selected class/endpoint
  // pairs. It uses only V5 candidate-comfort metrics and no legacy fixed-power
  // suffering/streak/spike semantics.
  val MigrationReferenceSlackSeconds = 60.0

  // Operational exact-search ceiling for the conservative V5 promotion gate.
  // This is not a preference threshold. Promotion fails closed if no valid
  // strict improvement is found inside this search horizon.
  val PromotionSearchSlackCeilingSeconds = 600.0


  def demandingAdjacencyCount(order: Vector[Int]): Int =
    order.sliding(2).count {
      case Vector(a, b) => isSteepTwistyTrail(a) && isSteepTwistyTrail(b)
      case _            => false
    }

  def warmupPrefixCount(order: Vector[Int]): Int =
    order.takeWhile(i => !isSteepTwistyTrail(i)).size

  def routeMaxUsedWallSeverity(route: HumanQualityRoute): Double =
    route.connectors
      .map(effectiveConnectorWallSeverity)
      .foldLeft(0.0)(math.max)

  def productRideQuality(route: HumanQualityRoute): ProductRideQuality =
    ProductRideQuality(
      maxCountedAscentM = route.climbShape.maxAscentM,
      upwardViolationM = route.climbShape.upwardViolationM,
      roughnessM = route.climbShape.roughnessM,
      totalCountedAscentM = route.climbShape.totalAscentM,
      roadStressSeconds = route.roadStressSeconds,
      candidateComfortSufferingSeconds = route.candidateComfortSufferingSeconds,
      downhillHandlingSeconds = route.connectors.map(_.downhillHandlingSeconds).sum,
      pathPenaltySeconds = route.connectors.map(_.transferQualityPenaltySeconds).sum
    )

  def migrationReferencePreferenceCompare(
      a: HumanQualityRoute,
      b: HumanQualityRoute
  ): Int =
    def cmpInt(x: Int, y: Int): Int =
      java.lang.Integer.compare(x, y)

    def cmpDouble(x: Double, y: Double): Int =
      java.lang.Double.compare(x, y)

    var c = cmpInt(a.warmup2Penalty, b.warmup2Penalty)
    if c == 0 then
      c = cmpInt(a.demandingAdjacencyCount, b.demandingAdjacencyCount)
    if c == 0 then
      c = cmpDouble(a.climbShape.maxAscentM, b.climbShape.maxAscentM)
    if c == 0 then
      c = cmpDouble(a.climbShape.upwardViolationM, b.climbShape.upwardViolationM)
    if c == 0 then
      c = cmpDouble(
        a.candidateComfortSufferingSeconds,
        b.candidateComfortSufferingSeconds
      )
    if c == 0 then
      c = cmpDouble(a.candidateLongestHighSeconds, b.candidateLongestHighSeconds)
    if c == 0 then
      c = cmpDouble(a.candidateLongestLowSeconds, b.candidateLongestLowSeconds)
    if c == 0 then
      c = cmpDouble(a.candidateSpikeLoadSeconds, b.candidateSpikeLoadSeconds)
    if c == 0 then
      c = cmpDouble(a.climbShape.roughnessM, b.climbShape.roughnessM)
    if c == 0 then
      c = cmpDouble(a.roadStressSeconds, b.roadStressSeconds)
    if c == 0 then
      c = cmpDouble(a.transferSeconds, b.transferSeconds)
    if c == 0 then
      c =
        a.order
          .map(_ + 1)
          .mkString(",")
          .compareTo(
            b.order
              .map(_ + 1)
              .mkString(",")
          )
    c

  def migrationReferencePreferenceBetterOrEqual(
      a: HumanQualityRoute,
      b: HumanQualityRoute
  ): Boolean =
    migrationReferencePreferenceCompare(a, b) <= 0


  case class HumanQualityShapeMemory(
      phase: Int,
      lastAscentBits: Long,
      lastDeltaBits: Long
  )

  def humanQualityShapeMemory(shape: ClimbShape): HumanQualityShapeMemory =
    shape.valuesM.size match
      case 0 =>
        HumanQualityShapeMemory(0, 0L, 0L)
      case 1 =>
        HumanQualityShapeMemory(
          1,
          java.lang.Double.doubleToLongBits(shape.valuesM.last),
          0L
        )
      case _ =>
        HumanQualityShapeMemory(
          2,
          java.lang.Double.doubleToLongBits(shape.valuesM.last),
          java.lang.Double.doubleToLongBits(shape.lastDeltaM.getOrElse(0.0))
        )

  def sameHumanQualityMemory(a: HumanQualityRoute, b: HumanQualityRoute): Boolean =
    humanQualityShapeMemory(a.climbShape) == humanQualityShapeMemory(b.climbShape)

  def migrationReferenceDominates(a: HumanQualityRoute, b: HumanQualityRoute): Boolean =
    sameHumanQualityMemory(a, b) &&
      a.warmup2Penalty <= b.warmup2Penalty &&
      a.demandingAdjacencyCount <= b.demandingAdjacencyCount &&
      a.climbShape.maxAscentM <= b.climbShape.maxAscentM + 1e-9 &&
      a.climbShape.upwardViolationM <= b.climbShape.upwardViolationM + 1e-9 &&
      a.candidateComfortSufferingSeconds <=
        b.candidateComfortSufferingSeconds + 1e-9 &&
      a.candidateLongestLowSeconds <= b.candidateLongestLowSeconds + 1e-9 &&
      a.candidateLongestHighSeconds <= b.candidateLongestHighSeconds + 1e-9 &&
      a.candidateSpikeLoadSeconds <= b.candidateSpikeLoadSeconds + 1e-9 &&
      a.climbShape.roughnessM <= b.climbShape.roughnessM + 1e-9 &&
      a.roadStressSeconds <= b.roadStressSeconds + 1e-9 &&
      a.transferSeconds <= b.transferSeconds + 1e-9

  case class SeverityCompletionFloor(
      minIncomingByTrail: Vector[Double],
      finishByTrail: Vector[Double]
  )

  def connectorMinSeconds(candidates: Vector[Connector]): Double =
    candidates.map(_.physicsSeconds).minOption.getOrElse(Double.PositiveInfinity)

  def severityCompletionFloor(
      maxSeverity: Double,
      endpointMode: RiderEndpointMode
  ): SeverityCompletionFloor =
    val minIncoming =
      trails.indices.map { target =>
        trails.indices
          .filter(_ != target)
          .map { source =>
            connectorMinSeconds(
              correctedBetweenVariantCache(source)(target)
                .filter(connector => correctedAllowedAtWallSeverity(connector, maxSeverity))
            )
          }
          .filter(Format.finite)
          .minOption
          .getOrElse(Double.PositiveInfinity)
      }.toVector
    val finishes =
      trails.indices.map { trailIndex =>
        connectorMinSeconds(
          correctedFinishVariantCache(trailIndex)
            .filter(connector => correctedAllowedAtWallSeverity(connector, maxSeverity))
            .filter(connector => endpointNumber(connector.route.to) == endpointMode.finishEndpoint)
        )
      }.toVector
    SeverityCompletionFloor(minIncoming, finishes)

  def severityCompletionLowerBound(
      floor: SeverityCompletionFloor,
      mask: Int,
      last: Int
  ): Double =
    if mask == fullMask then
      floor.finishByTrail(last)
    else
      var sum = 0.0
      var bestFinish = Double.PositiveInfinity
      var next = 0
      while next < trails.size do
        if (mask & trailBit(next)) == 0 then
          val incoming = floor.minIncomingByTrail(next)
          if !Format.finite(incoming) then
            return Double.PositiveInfinity
          sum += incoming
          bestFinish = math.min(bestFinish, floor.finishByTrail(next))
        next += 1
      if !Format.finite(bestFinish) then Double.PositiveInfinity
      else sum + bestFinish

  def rawChangedFrontierRows(
      series: TerrainFrontierSeries
  ): Vector[(Double, TerrainFrontierRoute)] =
    val out = Vector.newBuilder[(Double, TerrainFrontierRoute)]
    var previousReachable = Option.empty[TerrainFrontierRoute]
    series.rows.foreach { case (severity, result) =>
      result.foreach { current =>
        val changed =
          previousReachable match
            case None => true
            case Some(previous) =>
              math.abs(previous.transferSeconds - current.transferSeconds) > 1e-9 ||
                previous.order != current.order
        if changed then
          out += ((severity, current))
        previousReachable = Some(current)
      }
    }
    out.result()

  def exactMigrationReferenceAtSeverity(
      maxSeverity: Double,
      endpointMode: RiderEndpointMode,
      rawFastest: TerrainFrontierRoute,
      qualitySlackSeconds: Double
  ): HumanQualityRoute =
    require(
      qualitySlackSeconds >= 0.0 && Format.finite(qualitySlackSeconds),
      s"Human-quality slack must be finite and non-negative, got $qualitySlackSeconds"
    )
    val budgetSeconds = rawFastest.transferSeconds + qualitySlackSeconds
    val completionFloor = severityCompletionFloor(maxSeverity, endpointMode)

    def startVariants(trailIndex: Int): Vector[Connector] =
      correctedStartVariantCache(trailIndex)
        .filter(connector => correctedAllowedAtWallSeverity(connector, maxSeverity))
        .filter(connector => endpointNumber(connector.route.from) == endpointMode.startEndpoint)

    def betweenVariants(fromTrail: Int, toTrail: Int): Vector[Connector] =
      correctedBetweenVariantCache(fromTrail)(toTrail)
        .filter(connector => correctedAllowedAtWallSeverity(connector, maxSeverity))

    def finishVariants(trailIndex: Int): Vector[Connector] =
      correctedFinishVariantCache(trailIndex)
        .filter(connector => correctedAllowedAtWallSeverity(connector, maxSeverity))
        .filter(connector => endpointNumber(connector.route.to) == endpointMode.finishEndpoint)


    def canFinish(route: HumanQualityRoute): Boolean =
      val remaining =
        severityCompletionLowerBound(
          completionFloor,
          route.mask,
          route.last
        )
      Format.finite(remaining) &&
        route.transferSeconds + remaining <= budgetSeconds + 1e-9

    def putRoute(
        target: mutable.LongMap[mutable.ArrayBuffer[HumanQualityRoute]],
        route: HumanQualityRoute
    ): Unit =
      if canFinish(route) then
        val key = packedStateKey(route.mask, route.last)
        val frontier =
          target.getOrElseUpdate(
            key,
            mutable.ArrayBuffer.empty[HumanQualityRoute]
          )
        var i = 0
        var rejected = false
        while i < frontier.size && !rejected do
          val existing = frontier(i)
          if migrationReferenceDominates(existing, route) then
            val exactSame =
              migrationReferenceDominates(route, existing)
            if !exactSame ||
                migrationReferencePreferenceBetterOrEqual(existing, route)
            then
              rejected = true
          i += 1
        if !rejected then
          i = frontier.size - 1
          while i >= 0 do
            if migrationReferenceDominates(route, frontier(i)) then
              frontier.remove(i)
            i -= 1
          frontier += route

    var current =
      mutable.LongMap.empty[mutable.ArrayBuffer[HumanQualityRoute]]

    trails.indices.foreach { i =>
      if trailPositionAllowed(i, 1) then
        startVariants(i).foreach { connector =>
          putRoute(
            current,
            HumanQualityRoute(
              mask = trailBit(i),
              last = i,
              transferSeconds = connector.physicsSeconds,
              order = Vector(i),
              connectors = Vector(connector),
              warmup2Penalty = 0,
              demandingAdjacencyCount = 0,
              climbShape = emptyClimbShape,
              roadStressSeconds = connectorRoadStress(connector),
              candidateComfortSufferingSeconds =
                connector.candidateComfortPenaltySeconds,
              candidateLongestLowSeconds = connectorCandidateLongestLow(connector),
              candidateLongestHighSeconds = connectorCandidateLongestHigh(connector),
              candidateSpikeLoadSeconds = connector.candidateComfortSpikeLoadSeconds
            )
          )
        }
    }

    var layer = 1
    while layer < trailCount && current.nonEmpty do
      val nextLayer =
        mutable.LongMap.empty[mutable.ArrayBuffer[HumanQualityRoute]]
      current.foreach { case (_, frontier) =>
        frontier.foreach { base =>
          var next = 0
          while next < trails.size do
            if (base.mask & trailBit(next)) == 0 &&
                trailTransitionAllowed(base.last, next, layer + 1)
            then
              betweenVariants(base.last, next).foreach { connector =>
                val nextWarmupPenalty =
                  base.warmup2Penalty +
                    (if layer == 1 && isSteepTwistyTrail(next) then 1 else 0)
                val nextAdjacency =
                  base.demandingAdjacencyCount +
                    (
                      if isSteepTwistyTrail(base.last) &&
                          isSteepTwistyTrail(next)
                      then 1
                      else 0
                    )
                putRoute(
                  nextLayer,
                  HumanQualityRoute(
                    mask = base.mask | trailBit(next),
                    last = next,
                    transferSeconds = base.transferSeconds + connector.physicsSeconds,
                    order = base.order :+ next,
                    connectors = base.connectors :+ connector,
                    warmup2Penalty = nextWarmupPenalty,
                    demandingAdjacencyCount = nextAdjacency,
                    climbShape =
                      appendConnectorAscent(
                        base.climbShape,
                        connector,
                        zeroBasedTransferIndex = layer
                      ),
                    roadStressSeconds =
                      base.roadStressSeconds + connectorRoadStress(connector),
                    candidateComfortSufferingSeconds =
                      base.candidateComfortSufferingSeconds +
                        connector.candidateComfortPenaltySeconds,
                    candidateLongestLowSeconds =
                      math.max(
                        base.candidateLongestLowSeconds,
                        connectorCandidateLongestLow(connector)
                      ),
                    candidateLongestHighSeconds =
                      math.max(
                        base.candidateLongestHighSeconds,
                        connectorCandidateLongestHigh(connector)
                      ),
                    candidateSpikeLoadSeconds =
                      base.candidateSpikeLoadSeconds +
                        connector.candidateComfortSpikeLoadSeconds
                  )
                )
              }
            next += 1
        }
      }
      current = nextLayer
      layer += 1

    var best = Option.empty[HumanQualityRoute]

    current.foreach { case (_, frontier) =>
      frontier.foreach { base =>
        if base.mask == fullMask then
          finishVariants(base.last).foreach { connector =>
            val totalSeconds = base.transferSeconds + connector.physicsSeconds
            if totalSeconds <= budgetSeconds + 1e-9 then
              val candidate =
                base.copy(
                  transferSeconds = totalSeconds,
                  connectors = base.connectors :+ connector,
                  climbShape =
                    appendConnectorAscent(
                      base.climbShape,
                      connector,
                      zeroBasedTransferIndex = trailCount
                    ),
                  roadStressSeconds =
                    base.roadStressSeconds + connectorRoadStress(connector),
                  candidateComfortSufferingSeconds =
                    base.candidateComfortSufferingSeconds +
                      connector.candidateComfortPenaltySeconds,
                  candidateLongestLowSeconds =
                    math.max(
                      base.candidateLongestLowSeconds,
                      connectorCandidateLongestLow(connector)
                    ),
                  candidateLongestHighSeconds =
                    math.max(
                      base.candidateLongestHighSeconds,
                      connectorCandidateLongestHigh(connector)
                    ),
                  candidateSpikeLoadSeconds =
                    base.candidateSpikeLoadSeconds +
                      connector.candidateComfortSpikeLoadSeconds
                )
              best match
                case Some(existing)
                    if migrationReferencePreferenceBetterOrEqual(existing, candidate) =>
                  ()
                case _ =>
                  best = Some(candidate)
          }
      }
    }

    val selected =
      best.getOrElse(
        sys.error(
          f"No exact human-quality route fits wall<=${maxSeverity}%.6f, ${endpointMode.text}, " +
            s"slack=${Format.duration(qualitySlackSeconds)}, budget=${Format.duration(budgetSeconds)}."
        )
      )

    selected


  // ---------------------------------------------------------------------
  // EXACT V5 COMFORT PROMOTION
  //
  // Production uses one exact candidate-hard view. The previous broad research
  // views and percentage/knee reports are removed. For the selected class and
  // endpoint, labels are constrained by the migration reference guardrails and
  // searched inside PromotionSearchSlackCeilingSeconds. No beam, top-K,
  // quantization or random pruning is used.
  // ---------------------------------------------------------------------

  def compareComfortOrder(a: HumanQualityRoute, b: HumanQualityRoute): Int =
    a.order
      .map(_ + 1)
      .mkString(",")
      .compareTo(
        b.order
          .map(_ + 1)
          .mkString(",")
      )

  def compareComfortCandidateHard(a: HumanQualityRoute, b: HumanQualityRoute): Int =
    def ci(x: Int, y: Int) = java.lang.Integer.compare(x, y)
    def cd(x: Double, y: Double) = java.lang.Double.compare(x, y)
    var c = ci(a.warmup2Penalty, b.warmup2Penalty)
    if c == 0 then c = ci(a.demandingAdjacencyCount, b.demandingAdjacencyCount)
    if c == 0 then c = cd(a.candidateComfortSufferingSeconds, b.candidateComfortSufferingSeconds)
    if c == 0 then c = cd(a.roadStressSeconds, b.roadStressSeconds)
    if c == 0 then c = cd(a.climbShape.maxAscentM, b.climbShape.maxAscentM)
    if c == 0 then c = cd(a.climbShape.upwardViolationM, b.climbShape.upwardViolationM)
    if c == 0 then c = cd(a.climbShape.roughnessM, b.climbShape.roughnessM)
    if c == 0 then c = cd(a.transferSeconds, b.transferSeconds)
    if c == 0 then c = compareComfortOrder(a, b)
    c

  def comfortGuardResourcesNoWorse(
      a: HumanQualityRoute,
      b: HumanQualityRoute
  ): Boolean =
    a.warmup2Penalty <= b.warmup2Penalty &&
      a.demandingAdjacencyCount <= b.demandingAdjacencyCount &&
      a.roadStressSeconds <= b.roadStressSeconds + 1e-9 &&
      a.climbShape.maxAscentM <= b.climbShape.maxAscentM + 1e-9 &&
      a.climbShape.upwardViolationM <= b.climbShape.upwardViolationM + 1e-9 &&
      a.climbShape.roughnessM <= b.climbShape.roughnessM + 1e-9 &&
      a.candidateComfortSufferingSeconds <=
        b.candidateComfortSufferingSeconds + 1e-9 &&
      a.candidateLongestLowSeconds <=
        b.candidateLongestLowSeconds + 1e-9 &&
      a.candidateLongestHighSeconds <=
        b.candidateLongestHighSeconds + 1e-9 &&
      a.candidateSpikeLoadSeconds <=
        b.candidateSpikeLoadSeconds + 1e-9


  def promotedComfortStateDominates(
      a: HumanQualityRoute,
      b: HumanQualityRoute
  ): Boolean =
    sameHumanQualityMemory(a, b) &&
      compareComfortCandidateHard(a, b) <= 0 &&
      a.transferSeconds <= b.transferSeconds + 1e-9 &&
      comfortGuardResourcesNoWorse(a, b)

  def promotedComfortFinalDominates(
      a: HumanQualityRoute,
      b: HumanQualityRoute
  ): Boolean =
    compareComfortCandidateHard(a, b) <= 0 &&
      a.transferSeconds <= b.transferSeconds + 1e-9

  def humanQualityRouteFromConnectorSequence(
      order: Vector[Int],
      connectors: Vector[Connector]
  ): HumanQualityRoute =
    require(
      order.nonEmpty,
      "Human-quality connector sequence requires at least one mandatory trail."
    )
    require(
      connectors.size == order.size + 1,
      s"Expected ${order.size + 1} connectors for ${order.size} mandatory trails, got ${connectors.size}."
    )
    val shape =
      connectors.zipWithIndex.foldLeft(emptyClimbShape) {
        case (acc, (connector, transferIndex)) =>
          appendConnectorAscent(acc, connector, transferIndex)
      }
    HumanQualityRoute(
      mask = fullMask,
      last = order.last,
      transferSeconds = connectors.map(_.physicsSeconds).sum,
      order = order,
      connectors = connectors,
      warmup2Penalty =
        if warmupPrefixCount(order) >= 2 then 0 else 1,
      demandingAdjacencyCount = demandingAdjacencyCount(order),
      climbShape = shape,
      roadStressSeconds = connectors.map(connectorRoadStress).sum,
      candidateComfortSufferingSeconds =
        connectors.map(_.candidateComfortPenaltySeconds).sum,
      candidateLongestLowSeconds =
        connectors.map(connectorCandidateLongestLow).maxOption.getOrElse(0.0),
      candidateLongestHighSeconds =
        connectors.map(connectorCandidateLongestHigh).maxOption.getOrElse(0.0),
      candidateSpikeLoadSeconds =
        connectors.map(_.candidateComfortSpikeLoadSeconds).sum
    )

  def exactPromotedComfortFrontierAtSeverity(
      progressLabel: String,
      maxSeverity: Double,
      endpointMode: RiderEndpointMode,
      rawFastest: TerrainFrontierRoute,
      maxSlackSeconds: Double,
      guardBaseline: HumanQualityRoute
  ): Vector[HumanQualityRoute] =
    require(
      maxSlackSeconds >= 0.0 && Format.finite(maxSlackSeconds),
      s"Comfort-study slack must be finite and non-negative, got $maxSlackSeconds"
    )

    val startedNs = System.nanoTime()
    val budgetSeconds = rawFastest.transferSeconds + maxSlackSeconds
    val completionFloor = severityCompletionFloor(maxSeverity, endpointMode)

    def withinGuardrails(route: HumanQualityRoute): Boolean =
      route.warmup2Penalty <= guardBaseline.warmup2Penalty &&
        route.demandingAdjacencyCount <= guardBaseline.demandingAdjacencyCount &&
        route.roadStressSeconds <= guardBaseline.roadStressSeconds + 1e-9 &&
        route.climbShape.maxAscentM <= guardBaseline.climbShape.maxAscentM + 1e-9 &&
        route.climbShape.upwardViolationM <= guardBaseline.climbShape.upwardViolationM + 1e-9 &&
        route.climbShape.roughnessM <= guardBaseline.climbShape.roughnessM + 1e-9 &&
        route.candidateComfortSufferingSeconds <=
          guardBaseline.candidateComfortSufferingSeconds + 1e-9 &&
        route.candidateLongestLowSeconds <=
          guardBaseline.candidateLongestLowSeconds + 1e-9 &&
        route.candidateLongestHighSeconds <=
          guardBaseline.candidateLongestHighSeconds + 1e-9 &&
        route.candidateSpikeLoadSeconds <=
          guardBaseline.candidateSpikeLoadSeconds + 1e-9

    def guardAllows(route: HumanQualityRoute): Boolean =
      withinGuardrails(route)

    def startVariants(trailIndex: Int): Vector[Connector] =
      correctedStartVariantCache(trailIndex)
        .filter(connector => correctedAllowedAtWallSeverity(connector, maxSeverity))
        .filter(connector => endpointNumber(connector.route.from) == endpointMode.startEndpoint)

    def betweenVariants(fromTrail: Int, toTrail: Int): Vector[Connector] =
      correctedBetweenVariantCache(fromTrail)(toTrail)
        .filter(connector => correctedAllowedAtWallSeverity(connector, maxSeverity))

    def finishVariants(trailIndex: Int): Vector[Connector] =
      correctedFinishVariantCache(trailIndex)
        .filter(connector => correctedAllowedAtWallSeverity(connector, maxSeverity))
        .filter(connector => endpointNumber(connector.route.to) == endpointMode.finishEndpoint)

    def canFinish(route: HumanQualityRoute): Boolean =
      val remaining =
        severityCompletionLowerBound(
          completionFloor,
          route.mask,
          route.last
        )
      Format.finite(remaining) &&
        route.transferSeconds + remaining <= budgetSeconds + 1e-9

    def labelCount(
        states: mutable.LongMap[mutable.ArrayBuffer[HumanQualityRoute]]
    ): Int =
      states.valuesIterator.map(_.size).sum

    def printLayer(
        layer: Int,
        states: mutable.LongMap[mutable.ArrayBuffer[HumanQualityRoute]]
    ): Unit =
      println(
        f"[4/4 comfort $progressLabel%-18s RIDER-QUALITY  layer=$layer%2d/$trailCount%2d " +
          f"states=${states.size}%5d labels=${labelCount(states)}%7d " +
          f"elapsed=${timingSeconds(startedNs, System.nanoTime())}%.2f s"
      )

    def putRoute(
        target: mutable.LongMap[mutable.ArrayBuffer[HumanQualityRoute]],
        route: HumanQualityRoute
    ): Unit =
      if canFinish(route) && guardAllows(route) then
        val key = packedStateKey(route.mask, route.last)
        val frontier =
          target.getOrElseUpdate(
            key,
            mutable.ArrayBuffer.empty[HumanQualityRoute]
          )

        var rejected = false
        var i = 0
        while i < frontier.size && !rejected do
          if promotedComfortStateDominates(frontier(i), route) then
            rejected = true
          i += 1

        if !rejected then
          i = frontier.size - 1
          while i >= 0 do
            if promotedComfortStateDominates(route, frontier(i)) then
              frontier.remove(i)
            i -= 1
          frontier += route

    println(
      f"[4/4 comfort $progressLabel%-18s RIDER-QUALITY  START wall<=${maxSeverity}%.6f " +
        f"raw=${Format.duration(rawFastest.transferSeconds)}%s maxSlack=${Format.duration(maxSlackSeconds)}%s"
    )

    var current =
      mutable.LongMap.empty[mutable.ArrayBuffer[HumanQualityRoute]]

    trails.indices.foreach { i =>
      if trailPositionAllowed(i, 1) then
        startVariants(i).foreach { connector =>
          putRoute(
            current,
            HumanQualityRoute(
              mask = trailBit(i),
              last = i,
              transferSeconds = connector.physicsSeconds,
              order = Vector(i),
              connectors = Vector(connector),
              warmup2Penalty = 0,
              demandingAdjacencyCount = 0,
              climbShape = emptyClimbShape,
              roadStressSeconds = connectorRoadStress(connector),
              candidateComfortSufferingSeconds =
                connector.candidateComfortPenaltySeconds,
              candidateLongestLowSeconds =
                connectorCandidateLongestLow(connector),
              candidateLongestHighSeconds =
                connectorCandidateLongestHigh(connector),
              candidateSpikeLoadSeconds =
                connector.candidateComfortSpikeLoadSeconds
            )
          )
        }
    }
    printLayer(1, current)

    var layer = 1
    while layer < trailCount && current.nonEmpty do
      val nextLayer =
        mutable.LongMap.empty[mutable.ArrayBuffer[HumanQualityRoute]]

      current.foreach { case (_, frontier) =>
        frontier.foreach { base =>
          var next = 0
          while next < trails.size do
            if (base.mask & trailBit(next)) == 0 &&
                trailTransitionAllowed(base.last, next, layer + 1)
            then
              betweenVariants(base.last, next).foreach { connector =>
                val nextWarmupPenalty =
                  base.warmup2Penalty +
                    (if layer == 1 && isSteepTwistyTrail(next) then 1 else 0)
                val nextAdjacency =
                  base.demandingAdjacencyCount +
                    (
                      if isSteepTwistyTrail(base.last) &&
                          isSteepTwistyTrail(next)
                      then 1
                      else 0
                    )

                putRoute(
                  nextLayer,
                  HumanQualityRoute(
                    mask = base.mask | trailBit(next),
                    last = next,
                    transferSeconds = base.transferSeconds + connector.physicsSeconds,
                    order = base.order :+ next,
                    connectors = base.connectors :+ connector,
                    warmup2Penalty = nextWarmupPenalty,
                    demandingAdjacencyCount = nextAdjacency,
                    climbShape =
                      appendConnectorAscent(
                        base.climbShape,
                        connector,
                        zeroBasedTransferIndex = layer
                      ),
                    roadStressSeconds =
                      base.roadStressSeconds + connectorRoadStress(connector),
                    candidateComfortSufferingSeconds =
                      base.candidateComfortSufferingSeconds +
                        connector.candidateComfortPenaltySeconds,
                    candidateLongestLowSeconds =
                      math.max(
                        base.candidateLongestLowSeconds,
                        connectorCandidateLongestLow(connector)
                      ),
                    candidateLongestHighSeconds =
                      math.max(
                        base.candidateLongestHighSeconds,
                        connectorCandidateLongestHigh(connector)
                      ),
                    candidateSpikeLoadSeconds =
                      base.candidateSpikeLoadSeconds +
                        connector.candidateComfortSpikeLoadSeconds
                  )
                )
              }
            next += 1
        }
      }

      current = nextLayer
      layer += 1
      printLayer(layer, current)

    val finals = mutable.ArrayBuffer.empty[HumanQualityRoute]

    def putFinal(candidate: HumanQualityRoute): Unit =
      var rejected = false
      var i = 0
      while i < finals.size && !rejected do
        if promotedComfortFinalDominates(finals(i), candidate) then
          rejected = true
        i += 1

      if !rejected then
        i = finals.size - 1
        while i >= 0 do
          if promotedComfortFinalDominates(candidate, finals(i)) then
            finals.remove(i)
          i -= 1
        finals += candidate

    current.foreach { case (_, frontier) =>
      frontier.foreach { base =>
        if base.mask == fullMask then
          finishVariants(base.last).foreach { connector =>
            val totalSeconds = base.transferSeconds + connector.physicsSeconds
            if totalSeconds <= budgetSeconds + 1e-9 then
              val candidate =
                base.copy(
                  transferSeconds = totalSeconds,
                  connectors = base.connectors :+ connector,
                  climbShape =
                    appendConnectorAscent(
                      base.climbShape,
                      connector,
                      zeroBasedTransferIndex = trailCount
                    ),
                  roadStressSeconds =
                    base.roadStressSeconds + connectorRoadStress(connector),
                  candidateComfortSufferingSeconds =
                    base.candidateComfortSufferingSeconds +
                      connector.candidateComfortPenaltySeconds,
                  candidateLongestLowSeconds =
                    math.max(
                      base.candidateLongestLowSeconds,
                      connectorCandidateLongestLow(connector)
                    ),
                  candidateLongestHighSeconds =
                    math.max(
                      base.candidateLongestHighSeconds,
                      connectorCandidateLongestHigh(connector)
                    ),
                  candidateSpikeLoadSeconds =
                    base.candidateSpikeLoadSeconds +
                      connector.candidateComfortSpikeLoadSeconds
                )
              if guardAllows(candidate) then
                putFinal(candidate)
          }
      }
    }

    if finals.isEmpty then
      println(
        f"[4/4 comfort $progressLabel%-18s RIDER-QUALITY DONE finals=0 " +
          f"(no route satisfies migration guardrails) " +
          f"elapsed=${timingSeconds(startedNs, System.nanoTime())}%.2f s"
      )
    else
      println(
        f"[4/4 comfort $progressLabel%-18s RIDER-QUALITY DONE finals=${finals.size}%d " +
          f"elapsed=${timingSeconds(startedNs, System.nanoTime())}%.2f s"
      )

    finals.toVector

  case class EndpointRoleSeries(
      label: String,
      endpointMode: RiderEndpointMode
  )

  def rawUsefulTerrainLevels(
      series: TerrainFrontierSeries
  ): Vector[(Double, TerrainFrontierRoute)] =
    val out = Vector.newBuilder[(Double, TerrainFrontierRoute)]
    var previousUseful = Option.empty[TerrainFrontierRoute]
    rawChangedFrontierRows(series).foreach { case (severity, current) =>
      val gain =
        previousUseful.map(_.transferSeconds - current.transferSeconds)
      val changed =
        previousUseful.exists(_.order != current.order)
      val useful =
        previousUseful.isEmpty ||
          gain.exists(_ >= 180.0 - 1e-9) ||
          changed
      if useful then
        out += ((severity, current))
        previousUseful = Some(current)
    }
    out.result()

  val endpointRawUsefulLevels =
    exactWallBreakpointSeries.map { series =>
      (
        EndpointRoleSeries(series.label, series.endpointMode),
        rawUsefulTerrainLevels(series)
      )
    }

  val globalRawUsefulSeverities =
    endpointRawUsefulLevels
      .flatMap(_._2.map(_._1))
      .distinct
      .sorted

  require(
    globalRawUsefulSeverities.size >= 3,
    s"RAW endpoint-neutral product selector needs at least three distinct useful wall severities; " +
      s"found ${globalRawUsefulSeverities.mkString(",")}."
  )

  val productClassSeverities =
    Vector(
      globalRawUsefulSeverities.head,
      globalRawUsefulSeverities.drop(1).dropRight(1).last,
      globalRawUsefulSeverities.last
    )

  require(
    productClassSeverities.sliding(2).forall {
      case Vector(a, b) => b > a + 1e-9
      case _            => true
    },
    s"Product wall severities are not strictly increasing: ${productClassSeverities.mkString(",")}"
  )

  val loopEndpointRole =
    endpointRawUsefulLevels
      .map(_._1)
      .find(_.label.startsWith("LOOP"))
      .getOrElse(
        sys.error("Product selector requires LOOP terrain frontier.")
      )

  val p2pEndpointRole =
    endpointRawUsefulLevels
      .map(_._1)
      .find(_.label.startsWith("P2P"))
      .getOrElse(
        sys.error("Product selector requires P2P terrain frontier.")
      )

  def rawTerrainRouteAtSeverity(
      endpointMode: RiderEndpointMode,
      severity: Double
  ): Option[TerrainFrontierRoute] =
    exactWallBreakpointSeries
      .find(_.endpointMode == endpointMode)
      .flatMap(
        _.rows.collectFirst {
          case (candidateSeverity, route)
              if math.abs(candidateSeverity - severity) <= 1e-9 =>
            route
        }.flatten
      )

  case class ProductSetAssignment(
      endpointSeries: Vector[EndpointRoleSeries],
      severities: Vector[Double],
      rawRoutes: Vector[TerrainFrontierRoute]
  ):
    def p2pClassOneBased: Int =
      endpointSeries.indexWhere(_.label.startsWith("P2P")) + 1

    def totalTransferSeconds: Double =
      rawRoutes.map(_.transferSeconds).sum

    def totalRoadStressSeconds: Double =
      rawRoutes.flatMap(_.connectors).map(connectorRoadStress).sum

  val feasibleProductAssignments =
    productClassSeverities.indices.flatMap { p2pIndex =>
      val endpointSeries =
        productClassSeverities.indices.map { classIndex =>
          if classIndex == p2pIndex then p2pEndpointRole else loopEndpointRole
        }.toVector

      val rawRoutes =
        endpointSeries.zip(productClassSeverities).map {
          case (endpointSeries, severity) =>
            rawTerrainRouteAtSeverity(endpointSeries.endpointMode, severity)
        }

      if rawRoutes.forall(_.nonEmpty) then
        Some(
          ProductSetAssignment(
            endpointSeries = endpointSeries,
            severities = productClassSeverities,
            rawRoutes = rawRoutes.flatten
          )
        )
      else None
    }.toVector

  require(
    feasibleProductAssignments.nonEmpty,
    "No reachable endpoint-role assignment gives exactly two LOOP classes and one P2P class."
  )

  val selectedProductAssignment =
    feasibleProductAssignments.minBy { assignment =>
      (
        assignment.totalTransferSeconds,
        assignment.totalRoadStressSeconds,
        assignment.p2pClassOneBased
      )
    }

  val timingTerrainProductClassesDoneNs = System.nanoTime()
  println(
    "[3/4  50.0%] RAW terrain classes + endpoint-neutral assignment ready; " +
      "computing rider-quality routes."
  )

  def selectedTransferSeconds(route: MultiLabelRoute): Double =
    route.connectors.map(_.physicsSeconds).sum

  def productRouteToMultiLabel(route: HumanQualityRoute): MultiLabelRoute =
    val climbShape =
      route.connectors.zipWithIndex.foldLeft(emptyClimbShape) {
        case (shape, (connector, transferIndex)) =>
          appendConnectorAscent(shape, connector, transferIndex)
      }
    val trailRides = route.order.map(technicalTrailTimes)
    val trailSeconds = trailRides.map(_.totalSeconds).sum
    val connectorSeconds = route.connectors.map(_.physicsSeconds).sum
    val connectorMaxPower =
      route.connectors.map(_.maxRiderPowerW).foldLeft(0.0)(math.max)
    val trailMaxPower =
      trailRides.map(_.maxRiderPowerW).foldLeft(0.0)(math.max)
    MultiLabelRoute(
      mask = fullMask,
      order = route.order,
      connectors = route.connectors,
      movingSeconds = connectorSeconds + trailSeconds,
      candidateComfortSufferingSeconds = route.candidateComfortSufferingSeconds,
      candidateLongestLowSeconds = route.candidateLongestLowSeconds,
      candidateLongestHighSeconds = route.candidateLongestHighSeconds,
      candidateSpikeLoadSeconds = route.candidateSpikeLoadSeconds,
      downhillHandlingSeconds = route.connectors.map(_.downhillHandlingSeconds).sum,
      roadStressSeconds = route.connectors.map(connectorRoadStress).sum,
      recoveryBurdenSeconds = route.connectors.map(_.fatiguePenaltySeconds).sum,
      pathBurdenSeconds = route.connectors.map(_.transferQualityPenaltySeconds).sum,
      maxConnectorGrade100Pct =
        route.connectors.map(_.maxGrade100Pct).foldLeft(0.0)(math.max),
      maxRiderPowerW = math.max(connectorMaxPower, trailMaxPower),
      maxWallClass =
        route.connectors.map(finalEffectiveConnectorWallClass).foldLeft(0)(math.max),
      climbShape = climbShape,
      trailLateAscentBurden = route.order.map(technicalTrailLateAscentM).sum
    )

  val productC1Severity = selectedProductAssignment.severities(0)
  val productC2Severity = selectedProductAssignment.severities(1)
  val productC3Severity = selectedProductAssignment.severities(2)

  val referenceProductSelected =
    productClassSeverities.indices.map { classIndex =>
      val endpointMode =
        selectedProductAssignment.endpointSeries(classIndex).endpointMode
      val severity =
        productClassSeverities(classIndex)
      val rawFastest =
        selectedProductAssignment.rawRoutes(classIndex)
      val legacy =
        exactMigrationReferenceAtSeverity(
          severity,
          endpointMode,
          rawFastest,
          MigrationReferenceSlackSeconds
        )
      humanQualityRouteFromConnectorSequence(legacy.order, legacy.connectors)
    }.toVector

  def promotedMetricNoWorse(
      selected: HumanQualityRoute,
      reference: HumanQualityRoute
  ): Boolean =
    selected.warmup2Penalty <= reference.warmup2Penalty &&
      selected.demandingAdjacencyCount <= reference.demandingAdjacencyCount &&
      selected.roadStressSeconds <= reference.roadStressSeconds + 1e-9 &&
      selected.climbShape.maxAscentM <= reference.climbShape.maxAscentM + 1e-9 &&
      selected.climbShape.upwardViolationM <=
        reference.climbShape.upwardViolationM + 1e-9 &&
      selected.climbShape.roughnessM <= reference.climbShape.roughnessM + 1e-9 &&
      selected.candidateLongestLowSeconds <=
        reference.candidateLongestLowSeconds + 1e-9 &&
      selected.candidateLongestHighSeconds <=
        reference.candidateLongestHighSeconds + 1e-9 &&
      selected.candidateSpikeLoadSeconds <=
        reference.candidateSpikeLoadSeconds + 1e-9

  def promotedWallActive(
      classIndex: Int,
      route: HumanQualityRoute
  ): Boolean =
    if classIndex == 0 then true
    else
      routeMaxUsedWallSeverity(route) >
        productClassSeverities(classIndex - 1) + 1e-9

  def rawTerrainRouteForPromotion(
      endpointMode: RiderEndpointMode,
      severity: Double
  ): TerrainFrontierRoute =
    exactWallBreakpointSeries
      .find(_.endpointMode == endpointMode)
      .flatMap(
        _.rows.collectFirst {
          case (candidateSeverity, route)
              if math.abs(candidateSeverity - severity) <= 1e-9 =>
            route
        }.flatten
      )
      .getOrElse(
        sys.error(
          f"Promotion missing raw terrain route for ${endpointMode.text} wall<=$severity%.6f."
        )
      )

  def promotedProductRoute(classIndex: Int): HumanQualityRoute =
    val endpointSeries =
      selectedProductAssignment.endpointSeries(classIndex)
    val endpointMode =
      endpointSeries.endpointMode
    val severity =
      productClassSeverities(classIndex)
    val reference =
      referenceProductSelected(classIndex)
    val rawFastest =
      rawTerrainRouteForPromotion(endpointMode, severity)
    val frontier =
      exactPromotedComfortFrontierAtSeverity(
        progressLabel = s"RIDER-C${classIndex + 1}",
        maxSeverity = severity,
        endpointMode = endpointMode,
        rawFastest = rawFastest,
        maxSlackSeconds = PromotionSearchSlackCeilingSeconds,
        guardBaseline = reference
      )

    val selected =
      frontier
        .filter(route => promotedWallActive(classIndex, route))
        .filter(
          _.candidateComfortSufferingSeconds <
            reference.candidateComfortSufferingSeconds - 1e-9
        )
        .sortBy(route =>
          (
            route.transferSeconds,
            route.candidateComfortSufferingSeconds,
            route.roadStressSeconds,
            route.climbShape.maxAscentM,
            route.climbShape.upwardViolationM,
            route.climbShape.roughnessM
          )
        )
        .headOption
        .getOrElse(
          sys.error(
            s"Rider-quality selection fail-closed for C${classIndex + 1}: no route satisfies the configured quality constraints."
          )
        )

    require(
      promotedMetricNoWorse(selected, reference),
      s"Rider-quality selection regression: C${classIndex + 1} worsens a guarded rider metric."
    )
    require(
      promotedWallActive(classIndex, selected),
      s"Rider-quality selection regression: C${classIndex + 1} no longer requires its wall class."
    )
    require(
      selected.candidateComfortSufferingSeconds <
        reference.candidateComfortSufferingSeconds - 1e-9,
      s"Rider-quality selection regression: C${classIndex + 1} does not satisfy the candHard requirement."
    )

    selected

  val promotedProductSelected =
    productClassSeverities.indices.map(promotedProductRoute).toVector


  val productC1Selected = promotedProductSelected(0)
  val productC2Selected = promotedProductSelected(1)
  val productC3Selected = promotedProductSelected(2)

  val productC1Route = productRouteToMultiLabel(productC1Selected)
  val productC2Route = productRouteToMultiLabel(productC2Selected)
  val productC3Route = productRouteToMultiLabel(productC3Selected)

  val timingExactDoneNs = System.nanoTime()
  println("[3/4  75.0%] Product set selected; reconstructing and auditing three rider GPXs.")

  case class RiderProfileSpec(
      label: String,
      route: MultiLabelRoute,
      recommended: Boolean,
      gpxPath: Path,
      wallSeverityCeiling: Double,
      productRole: String
  )

  def endpointRoleText(series: EndpointRoleSeries): String =
    if series.label.startsWith("P2P") then "P2P" else "LOOP"

  val selectedClassEndpointSeries =
    selectedProductAssignment.endpointSeries

  val riderProfileSpecs =
    Vector(
      RiderProfileSpec(
        label = "DAY-C1",
        route = productC1Route,
        recommended = true,
        gpxPath = cfg.out,
        wallSeverityCeiling = productC1Severity,
        productRole = endpointRoleText(selectedClassEndpointSeries(0))
      ),
      RiderProfileSpec(
        label = "DAY-C2",
        route = productC2Route,
        recommended = false,
        gpxPath = wallC2GpxPath,
        wallSeverityCeiling = productC2Severity,
        productRole = endpointRoleText(selectedClassEndpointSeries(1))
      ),
      RiderProfileSpec(
        label = "DAY-C3",
        route = productC3Route,
        recommended = false,
        gpxPath = wallC3GpxPath,
        wallSeverityCeiling = productC3Severity,
        productRole = endpointRoleText(selectedClassEndpointSeries(2))
      )
    )


  def routeMatrix(route: MultiLabelRoute): Matrix =
    route.connectors.zipWithIndex.foldLeft(matrix) {
      case (current, (connector, 0)) =>
        current.copy(startToTrail = current.startToTrail.updated(route.order.head, Some(connector)))
      case (current, (connector, step)) if step < route.order.size =>
        val from = route.order(step - 1)
        val to = route.order(step)
        current.copy(between = current.between.updated(from, current.between(from).updated(to, Some(connector))))
      case (current, (connector, _)) =>
        current.copy(trailToFinish = current.trailToFinish.updated(route.order.last, Some(connector)))
    }

  case class ProfileWritten(
      spec: RiderProfileSpec,
      route: MultiLabelRoute,
      gpxPath: Path,
      transitions: Vector[Output.Transition],
      outputPoints: Vector[Point],
      rideTime: RideTimeEstimate,
      audit: AuditResult
  ):
    def elapsedSeconds: Double =
      rideTime.totalSeconds +
        route.order.size *
          2.0 *
          cfg.trailPauseMin *
          60.0

    def totalDistanceKm: Double =
      Geometry.pathLengthMeters(
        outputPoints
      ) /
        1000.0

    def transferAscentM: Double =
      transitions.map(
        _.connector.ascentM
      ).sum

    def selectedStartPoint: Point =
      route.connectors.head.route.from

    def selectedEndPoint: Point =
      route.connectors.last.route.to

    def roadStressSeconds: Double =
      transitions.map(
        t =>
          connectorRoadStress(t.connector)
      ).sum

    def steepestTransition: Option[Output.Transition] =
      transitions.sortBy(
        t =>
          -t.connector.maxGrade100Pct
      ).headOption

  def writeProfile(spec: RiderProfileSpec): ProfileWritten =
    val route = spec.route
    val candidateMatrix = routeMatrix(route)
    val selectedStartPoint = route.connectors.headOption
        .map(_.route.from)
        .getOrElse(endpoint1)
    val selectedEndPoint = route.connectors.lastOption
        .map(_.route.to)
        .getOrElse(endpoint1)
    val selectedCfg =
      cfg.copy(
        start = selectedStartPoint,
        startName =
          endpointName(
            selectedStartPoint
          ),
        finish = selectedEndPoint,
        finishName = endpointName(selectedEndPoint)
      )
    val selectedFinish =
      endpointTrail(
        endpointNumber(
          selectedEndPoint
        ),
        selectedEndPoint,
        endpointName(selectedEndPoint)
      )
    val (outputPoints, transitions) =
      Output.assemble(
        selectedCfg,
        trails,
        endpointName(
          selectedStartPoint
        ),
        selectedFinish,
        candidateMatrix,
        route.order
      )
    val connectorTimes = transitions.map { transition =>
        val connector = transition.connector
        val recomputed =
          RidePhysics.estimate(
            connector.route.points,
            cfg,
            downhillCapKph = None,
            crrOverride = connector.effectiveCrr
          )
        recomputed
      }
    val selectedTrailTimes =
      route.order.map { i =>
          technicalTrailTimes(i)
      }
    val rideTime =
      RidePhysics.combine(
        connectorTimes ++
          selectedTrailTimes
      )
    val audit =
      Audit.run(
        cfg = selectedCfg,
        trails = trails,
        forbiddenTransferTrails = forbiddenTransferTrails,
        finish = selectedFinish,
        order = route.order,
        transitions = transitions,
        outputPoints = outputPoints
      )
    val title = spec.label +
        (
          if spec.recommended then " — recommended"
          else " — alternative"
        )
    val description = s"$title. " +
        f"Terrain wall severity ceiling ${spec.wallSeverityCeiling}%.3f. " +
        s"Endpoint role ${spec.productRole}. " +
        s"${route.order.size} mandatory technical trails. " +
        s"Moving ${Format.duration(rideTime.totalSeconds)}. " +
        s"Downhill handling ${Format.duration(route.downhillHandlingSeconds)}. " +
        s"Road stress ${Format.duration(route.roadStressSeconds)}. " +
        s"Order: " +
        (
          Vector(
            endpointName(selectedStartPoint)
          ) ++
            route.order.map(
              i =>
                trails(i).name
            ) ++
            Vector(endpointName(selectedEndPoint))
        ).mkString(" -> ")
    Gpx.write(spec.gpxPath, title, description, outputPoints)
    ProfileWritten(
      spec = spec,
      route = route,
      gpxPath = spec.gpxPath,
      transitions = transitions,
      outputPoints = outputPoints,
      rideTime = rideTime,
      audit = audit
    )


  val writtenProfiles = riderProfileSpecs.map(writeProfile)

  val timingProfilesDoneNs = System.nanoTime()
  println("[3/4 100.0%] Rider GPXs reconstructed and audited.")

  val recommendedWritten =
    writtenProfiles.find(
      _.spec.recommended
    ).getOrElse(sys.error("Recommended unique rider profile disappeared during reconstruction."))

  // NON-DESTRUCTIVE OUTPUT OWNERSHIP:
  // The planner writes the current declared output/report paths, but it never
  // deletes stale, duplicate, historical, or audit-FAIL files. Historical
  // suffixes remain in the input-exclusion set only so an old planner artifact
  // cannot become a mandatory technical trail on a later run.

  val automaticRecommendationAllowed =
    recommendedWritten.audit.verdict != "FAIL"

  def selectedNames(route: MultiLabelRoute): Vector[String] =
    route.order.map { i =>
        trails(i).name
    }

  def orderText(route: MultiLabelRoute): String =
    (
      Vector(
        endpointName(route.connectors.head.route.from)
      ) ++
        selectedNames(
          route
        ) ++
        Vector(endpointName(route.connectors.last.route.to))
    ).mkString(" -> ")

  def gpxFileName(path: Path): String =
    path.getFileName.toString

  def warningPriority(warning: String): Int =
    val w = warning.toLowerCase
    if w.contains("motorway") ||
        w.contains("trunk") ||
        w.contains("trace_attributes") ||
        w.contains("trail overlap") ||
        w.contains("point gap")
    then
      0
    else if w.contains("primary-road") ||
        w.contains("primary road")
    then
      1
    else if w.contains("max sustained 100 m")
    then
      2
    else if w.contains("long connector climb")
    then
      3
    else
      4

  def watchOutLines(written: ProfileWritten, limit: Int): Vector[String] =
    val sorted =
      written.audit.warnings.sortBy(
        warning =>
          (warningPriority(warning), warning)
      )
    val visible = sorted.take(limit)
    if visible.nonEmpty then
      if sorted.size > limit then
        visible :+
          s"... ${sorted.size - limit} more warning(s); see day.debug.txt"
      else
        visible
    else
      written.steepestTransition.toVector.map { transition =>
        f"${transition.label}: max sustained 100 m uphill grade " +
          f"${transition.connector.maxGrade100Pct}%.1f%%"
      }

  def steepestConnector100Pct(written: ProfileWritten): Double =
    written.steepestTransition
      .map(_.connector.maxGrade100Pct)
      .getOrElse(0.0)

  def humanReport(): String =
    val sb = new StringBuilder
    sb.append(
      "MTB DAY PLAN\n" +
        "============\n" +
        s"Planner build: ${BuildInfo.id}\n\n" +
        s"Mandatory technical GPXs: $trailCount.\n" +
        s"Selected endpoint roles: ${riderProfileSpecs.map(spec => s"${spec.label} ${endpointName(spec.route.connectors.head.route.from)} -> ${endpointName(spec.route.connectors.last.route.to)}").mkString("; ")}.\n\n"
    )
    writtenProfiles.foreach { written =>
      val role =
        if written.spec.recommended && automaticRecommendationAllowed then
          "RECOMMENDED"
        else if written.audit.verdict == "FAIL" then
          "DIAGNOSTIC — DO NOT RIDE"
        else
          "ALTERNATIVE"
      sb.append(
        s"${written.spec.label} — $role — ${gpxFileName(written.gpxPath)}\n" +
          f"  ${written.totalDistanceKm}%.2f km | moving ${Format.duration(written.rideTime.totalSeconds)}%s | planned ${Format.duration(written.elapsedSeconds)}%s | transfer +${written.transferAscentM}%.0f m\n" +
          s"  candHard ${Format.duration(written.route.candidateComfortSufferingSeconds)} | road ${Format.duration(written.roadStressSeconds)} | " +
          f"max100 ${steepestConnector100Pct(written)}%.1f%% | audit ${written.audit.verdict}\n" +
          s"  ${endpointName(written.selectedStartPoint)} -> ${endpointName(written.selectedEndPoint)}\n" +
          s"  ${orderText(written.route)}\n"
      )
      watchOutLines(
        written,
        4
      ).foreach { warning =>
          sb.append(s"  WATCH: $warning\n")
      }
      sb.append("\n")
    }
    if recommendedWritten.audit.verdict == "FAIL" then
      sb.append("!!! RECOMMENDED AUDIT FAIL — DO NOT RIDE UNTIL DEBUGGED. !!!\n\n")
    else if recommendedWritten.audit.verdict == "WARN" then
      sb.append("Recommended route audit is WARN; review WATCH lines above and day.debug.txt before riding.\n\n")
    else
      sb.append("Recommended route audit: PASS.\n\n")
    sb.append(
      "FILES\n" +
        "-----\n" +
        s"C1 day variant: ${gpxFileName(writtenProfiles(0).gpxPath)}\n" +
        s"C2 day variant: ${gpxFileName(writtenProfiles(1).gpxPath)}\n" +
        s"C3 day variant: ${gpxFileName(writtenProfiles(2).gpxPath)}\n" +
        s"Diagnostics: ${debugPath.getFileName}\n"
    )
    sb.result()

  def debugReport(): String =
    val sb = new StringBuilder
    val finalAvoidFailures = writtenProfiles.flatMap(_.audit.failures).count(_.contains("forbidden transfer corridor"))
    val finalAvoidWarnings = writtenProfiles.flatMap(_.audit.warnings).count(_.contains("forbidden transfer corridor"))

    def orderNames(order: Vector[Int]): String =
      order.map(i => trails(i).name).mkString(" -> ")

    def productLine(spec: RiderProfileSpec): String =
      val q = ProductRideQuality(
        maxCountedAscentM = spec.route.climbShape.maxAscentM,
        upwardViolationM = spec.route.climbShape.upwardViolationM,
        roughnessM = spec.route.climbShape.roughnessM,
        totalCountedAscentM = spec.route.climbShape.totalAscentM,
        roadStressSeconds = spec.route.roadStressSeconds,
        candidateComfortSufferingSeconds =
          spec.route.candidateComfortSufferingSeconds,
        downhillHandlingSeconds = spec.route.downhillHandlingSeconds,
        pathPenaltySeconds = spec.route.pathBurdenSeconds
      )
      f"${spec.label}%-6s | ${spec.productRole}%-4s | wall<=${spec.wallSeverityCeiling}%.6f | " +
        f"transfer=${Format.duration(selectedTransferSeconds(spec.route))}%s | maxA=${q.maxCountedAscentM}%.0f m | " +
        f"up=${q.upwardViolationM}%.0f m | rough=${q.roughnessM}%.0f | road=${Format.duration(q.roadStressSeconds)}%s | " +
        f"candHard=${Format.duration(q.candidateComfortSufferingSeconds)}%s | order: ${orderNames(spec.route.order)}"

    sb.append(
      "MTB CANONICAL RUN\n" +
        "=================\n\n" +
        s"Planner build: ${BuildInfo.id}\n" +
        s"Valhalla: $fingerprint\n" +
        s"Mandatory GPXs: $trailCount\n" +
        s"State masks: $stateCount\n" +
        s"Routing profiles: ${RouteSearchProfiles.all.size}\n\n"
    )

    sb.append(
      "INPUT / AUDIT SNAPSHOT\n" +
        "----------------------\n" +
        s"Mandatory technical GPX: ${trails.size}.\n" +
        s"Avoid GPX: ${forbiddenTransferTrails.size}.\n" +
        s"Real-ride GPX: ${realRides.size}.\n" +
        s"Final avoid failures: $finalAvoidFailures; junction warnings: $finalAvoidWarnings.\n" +
        s"Real-ride evidence records: ${productionRealRideWallEvidence.size}.\n\n"
    )

    sb.append(
      "ROUTES\n" +
        "------\n" +
        riderProfileSpecs.map(productLine).mkString("\n") +
        "\n\n"
    )

    sb.append(
      "ENDPOINT ASSIGNMENT\n" +
        "-------------------\n" +
        s"Class severities: ${productClassSeverities.map(s => f"$s%.6f").mkString(" < ")}.\n" +
        s"Feasible endpoint assignments: ${feasibleProductAssignments.size}; " +
        s"selected P2P class=C${selectedProductAssignment.p2pClassOneBased}.\n"
    )
    productClassSeverities.indices.foreach { i =>
      val endpointSeries = selectedProductAssignment.endpointSeries(i)
      sb.append(
        f"  C${i + 1}%d wall<=${productClassSeverities(i)}%.6f -> ${endpointRoleText(endpointSeries)}%-4s | ${endpointSeries.endpointMode.text}\n"
      )
    }
    sb.append("\n")

    sb.append(
      "RAW CLASS x ENDPOINT EVIDENCE MATRIX\n" +
        "------------------------------------\n"
    )
    productClassSeverities.zipWithIndex.foreach { case(severity,classIndex) =>
      Vector(loopEndpointRole,p2pEndpointRole).foreach { endpointSeries =>
        val mode = endpointRoleText(endpointSeries)
        rawTerrainRouteAtSeverity(endpointSeries.endpointMode,severity) match
          case None =>
            sb.append(
              f"  C${classIndex + 1}%d $mode%-4s wall<=${severity}%.9f UNREACHABLE\n"
            )
          case Some(route) =>
            sb.append(
              f"  C${classIndex + 1}%d $mode%-4s wall<=${severity}%.9f transfer=${route.transferSeconds}%.3f " +
                f"road=${route.connectors.map(connectorRoadStress).sum}%.3f usedWall=${route.maxUsedSeverity}%.9f order=${orderNames(route.order)}\n"
            )
            route.connectors.zipWithIndex.foreach { case(connector,connectorIndex) =>
              val evidenceFloor = realRideWallSeverityFloor(connector)
              val transitionLabel =
                if connectorIndex == 0 then
                  s"START->${trails(route.order.head).name}"
                else if connectorIndex == route.connectors.size - 1 then
                  s"${trails(route.order.last).name}->FINISH_$mode"
                else
                  s"${trails(route.order(connectorIndex - 1)).name}->${trails(route.order(connectorIndex)).name}"
              sb.append(
                f"    $transitionLabel profile=v${connector.routingSpeedKph}%.0f-h${connector.routingUseHills}%.2f-r${connector.routingUseRoads}%.2f " +
                  f"distance=${connector.route.lengthKm * 1000.0}%.1f transfer=${connector.physicsSeconds}%.3f " +
                  f"road=${connectorRoadStress(connector)}%.3f wall=${effectiveConnectorWallSeverity(connector)}%.9f " +
                  f"physical=${connectorWallSeverity(connector)}%.9f evidence=${evidenceFloor}%.9f " +
                  f"max30=${connector.maxGrade30Pct}%.3f max100=${connector.maxGrade100Pct}%.3f " +
                  f"p180=${connector.longestPowerStreakSecondsByThreshold.getOrElse(PowerPolicy.SafetyWallPowerW,0.0)}%.3f " +
                  f"ascent=${connector.ascentM}%.1f crr=${connector.effectiveCrr}%.5f\n"
              )
            }
      }
    }
    sb.append("END RAW CLASS x ENDPOINT EVIDENCE MATRIX\n\n")

    sb.append(
      "POWER METRICS\n" +
        "-------------\n" +
        s"candHard thresholds: ${PowerPolicy.candidateComfortThresholdsW(cfg.riderPowerW).mkString("/", "/", " W")}.\n" +
        s"streak thresholds: ${PowerPolicy.candidateStopStreakThresholdsW(cfg.riderPowerW).mkString("/", "/", " W")}.\n" +
        f"spike base/scale: ${PowerPolicy.candidateSpikeBaseW(cfg.riderPowerW)}%.0f/${PowerPolicy.candidateSpikeScaleW(cfg.riderPowerW)}%.0f W.\n" +
        s"hard-safety streak: ${PowerPolicy.SafetyWallPowerW} W/${PowerPolicy.SafetyWallMinStreakSeconds.toInt} s.\n\n"
    )

    sb.append(
      "REAL-RIDE EVIDENCE\n" +
        "------------------\n" +
        s"Evidence records: ${productionRealRideWallEvidence.size}.\n" +
        s"Connector variants with applied evidence floor: ${realRideWallSeverityFloorByConnector.size()}.\n\n"
    )

    sb.append(
      "FINAL RIDER AUDITS\n" +
        "------------------\n"
    )
    writtenProfiles.foreach { written =>
      sb.append(
        s"\n${written.spec.label}: ${written.audit.verdict}\n" +
          s"GPX: ${gpxFileName(written.gpxPath)}\n" +
          s"Order: ${orderText(written.route)}\n" +
          s"Audit failures: ${written.audit.failures.size}; warnings: ${written.audit.warnings.size}\n"
      )
      written.audit.failures.foreach(failure => sb.append(s"FAIL: $failure\n"))
      written.audit.warnings.take(30).foreach(warning => sb.append(s"WARN: $warning\n"))
      if written.audit.warnings.size > 30 then
        sb.append(s"... ${written.audit.warnings.size - 30} more warning(s) omitted from compact debug.\n")
    }

    sb.append(
      "\nOLD ROUTE-DERIVED BLOCKER EVIDENCE\n" +
        "-----------------------------------\n"
    )
    val blockerEvidence = DifferentialEvidence.snapshot
    if blockerEvidence.isEmpty then sb.append("No targeted blocker evidence captured.\n")
    else blockerEvidence.foreach(line => sb.append(line).append("\n"))

    sb.append(
      "\nTIMING\n" +
        "------\n" +
        f"startup + connector matrix: ${timingSeconds(timingRunStartedNs, timingMatrixReadyNs)}%.2f s\n" +
        f"matrix -> terrain frontier ready: ${timingSeconds(timingMatrixReadyNs, timingTerrainFrontierDoneNs)}%.2f s\n" +
        f"terrain frontier -> terrain product classes ready: ${timingSeconds(timingTerrainFrontierDoneNs, timingTerrainProductClassesDoneNs)}%.2f s\n" +
        f"terrain classes -> rider set selected: ${timingSeconds(timingTerrainProductClassesDoneNs, timingExactDoneNs)}%.2f s\n" +
        f"reconstruct + final audits: ${timingSeconds(timingExactDoneNs, timingProfilesDoneNs)}%.2f s\n" +
        f"total to completed rider profiles: ${timingSeconds(timingRunStartedNs, timingProfilesDoneNs)}%.2f s\n" +
        "Report/debug writing is excluded from the rider-profile total.\n"
    )

    sb.result()

  val reportText = humanReport()
  val debugText = debugReport()
  writeText(baseReportPath, reportText)
  writeText(debugPath, debugText)
  println(
    s"DONE: ${recommendedWritten.spec.label} audit=${recommendedWritten.audit.verdict}; " +
      f"runtime=${timingSeconds(timingRunStartedNs, System.nanoTime())}%.2f s; " +
      s"GPX=${gpxFileName(recommendedWritten.gpxPath)}; report=${baseReportPath.getFileName}; debug=${debugPath.getFileName}"
  )

  if recommendedWritten.audit.verdict == "FAIL"
  then Console.err.println("WARNING: recommended profile audit FAIL. Do not ride before inspecting the debug report.")
