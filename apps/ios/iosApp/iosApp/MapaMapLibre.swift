import Foundation
import MapLibre
import Network
import Shared
import UIKit

private let fuenteTrazos = "gacelas-trazos"
private let fuenteCirculos = "gacelas-circulos"

/// MLNMapView con el estilo Gacelas y las capas GeoJSON de trazos y círculos (igual que MapaLibre en Android).
final class VistaMapaGacelas: MLNMapView, MLNMapViewDelegate {
    var oyente: OyenteMapa?
    private var trazosPendientes = "{\"type\":\"FeatureCollection\",\"features\":[]}"
    private var circulosPendientes = "{\"type\":\"FeatureCollection\",\"features\":[]}"
    private var estiloListo = false
    private var archivoTemporal: URL?

    init(oscuro: Bool, centro: CLLocationCoordinate2D, zoom: Double, estiloJson: String?, oyente: OyenteMapa) {
        var url = Bundle.main.url(forResource: oscuro ? "gacelas-oscuro" : "gacelas-claro", withExtension: "json")
        if let json = estiloJson {
            // Sin red: estilo con la version exacta de teselas descargada (ReglasTeselas.fijarTeselas).
            let tmp = FileManager.default.temporaryDirectory.appendingPathComponent("gacelas-sin-red-\(UUID().uuidString).json")
            if (try? json.write(to: tmp, atomically: true, encoding: .utf8)) != nil { url = tmp; archivoTemporal = tmp }
        }
        super.init(frame: .zero, styleURL: url)
        self.oyente = oyente
        delegate = self
        logoView.isHidden = true
        compassView.isHidden = true
        allowsRotating = true
        setCenter(centro, zoomLevel: zoom, animated: false)
        let toque = UITapGestureRecognizer(target: self, action: #selector(tocar(_:)))
        gestureRecognizers?.compactMap { $0 as? UITapGestureRecognizer }.forEach { toque.require(toFail: $0) }
        addGestureRecognizer(toque)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) no se usa") }

    deinit { if let t = archivoTemporal { try? FileManager.default.removeItem(at: t) } }

    func mapView(_ mapView: MLNMapView, didFinishLoading style: MLNStyle) {
        let trazos = MLNShapeSource(identifier: fuenteTrazos, shape: nil, options: nil)
        let circulos = MLNShapeSource(identifier: fuenteCirculos, shape: nil, options: nil)
        style.addSource(trazos)
        style.addSource(circulos)
        let linea = MLNLineStyleLayer(identifier: fuenteTrazos, source: trazos)
        linea.lineColor = NSExpression(mglJSONObject: ["to-color", ["get", "color"]])
        linea.lineWidth = NSExpression(mglJSONObject: ["get", "ancho"])
        linea.lineOpacity = NSExpression(mglJSONObject: ["get", "op"])
        linea.lineCap = NSExpression(forConstantValue: "round")
        linea.lineJoin = NSExpression(forConstantValue: "round")
        style.addLayer(linea)
        let circulo = MLNCircleStyleLayer(identifier: fuenteCirculos, source: circulos)
        circulo.circleColor = NSExpression(mglJSONObject: ["to-color", ["get", "color"]])
        circulo.circleRadius = NSExpression(mglJSONObject: ["get", "r"])
        circulo.circleOpacity = NSExpression(mglJSONObject: ["get", "op"])
        circulo.circleStrokeColor = NSExpression(mglJSONObject: ["to-color", ["get", "borde"]])
        circulo.circleStrokeWidth = NSExpression(mglJSONObject: ["get", "bw"])
        style.addLayer(circulo)
        estiloListo = true
        poner(fuenteTrazos, trazosPendientes)
        poner(fuenteCirculos, circulosPendientes)
    }

    func ponerTrazos(_ geojson: String) { trazosPendientes = geojson; if estiloListo { poner(fuenteTrazos, geojson) } }
    func ponerCirculos(_ geojson: String) { circulosPendientes = geojson; if estiloListo { poner(fuenteCirculos, geojson) } }

    private func poner(_ fuente: String, _ geojson: String) {
        guard let src = style?.source(withIdentifier: fuente) as? MLNShapeSource,
              let datos = geojson.data(using: .utf8),
              let forma = try? MLNShape(data: datos, encoding: String.Encoding.utf8.rawValue) else { return }
        src.shape = forma
    }

    private static let gestos: MLNCameraChangeReason = [.gesturePan, .gesturePinch, .gestureRotate, .gestureZoomIn, .gestureZoomOut, .gestureOneFingerZoom, .gestureTilt]

    func mapView(_ mapView: MLNMapView, regionWillChangeWith reason: MLNCameraChangeReason, animated: Bool) {
        if !reason.intersection(Self.gestos).isEmpty { oyente?.gesto() }
    }

    func mapViewRegionIsChanging(_ mapView: MLNMapView) { oyente?.camara(rumbo: mapView.direction) }

    func mapView(_ mapView: MLNMapView, regionDidChangeAnimated animated: Bool) { oyente?.camara(rumbo: mapView.direction) }

    @objc private func tocar(_ g: UITapGestureRecognizer) {
        let p = g.location(in: self)
        let marco = CGRect(x: p.x - 12, y: p.y - 12, width: 24, height: 24)
        if let id = visibleFeatures(in: marco, styleLayerIdentifiers: [fuenteCirculos]).compactMap({ $0.attribute(forKey: "id") as? String }).first {
            oyente?.tocoCirculo(id: id)
        }
    }
}

/// Implementa el puente Kotlin (PuenteMapa) con MapLibre Native iOS.
final class PuenteMapLibre: NSObject, PuenteMapa {
    func crear(oscuro: Bool, lat: Double, lng: Double, zoom: Double, estiloJson: String?, oyente: OyenteMapa) -> UIView {
        VistaMapaGacelas(oscuro: oscuro, centro: CLLocationCoordinate2D(latitude: lat, longitude: lng), zoom: zoom, estiloJson: estiloJson, oyente: oyente)
    }

    func trazos(vista: UIView, geojson: String) { (vista as? VistaMapaGacelas)?.ponerTrazos(geojson) }
    func circulos(vista: UIView, geojson: String) { (vista as? VistaMapaGacelas)?.ponerCirculos(geojson) }

    func camara(vista: UIView, lat: Double, lng: Double, zoom: Double, rumbo: Double, inclinacion: Double, arribaPx: Double, ms: Int32, lineal: Bool) {
        guard let m = vista as? MLNMapView else { return }
        let centro = CLLocationCoordinate2D(latitude: lat, longitude: lng)
        let tam = m.bounds.size == .zero ? CGSize(width: 390, height: 844) : m.bounds.size
        let altitud = MLNAltitudeForZoomLevel(zoom, CGFloat(inclinacion), lat, tam)
        let cam = MLNMapCamera(lookingAtCenter: centro, altitude: altitud, pitch: CGFloat(inclinacion), heading: rumbo)
        let tiempo = CAMediaTimingFunction(name: lineal ? .linear : .easeInEaseOut)
        m.setCamera(cam, withDuration: Double(ms) / 1000.0, animationTimingFunction: tiempo,
                    edgePadding: UIEdgeInsets(top: CGFloat(arribaPx), left: 0, bottom: 0, right: 0), completionHandler: nil)
    }

    func encuadrar(vista: UIView, sur: Double, oeste: Double, norte: Double, este: Double, paddingDp: Double) {
        guard let m = vista as? MLNMapView else { return }
        let b = MLNCoordinateBounds(sw: CLLocationCoordinate2D(latitude: sur, longitude: oeste), ne: CLLocationCoordinate2D(latitude: norte, longitude: este))
        let p = CGFloat(paddingDp)
        m.setVisibleCoordinateBounds(b, edgePadding: UIEdgeInsets(top: p, left: p, bottom: p, right: p), animated: true, completionHandler: nil)
    }

    func norte(vista: UIView, plano: Bool) {
        guard let m = vista as? MLNMapView else { return }
        let cam = m.camera
        cam.heading = 0
        if plano { cam.pitch = 0 }
        m.setCamera(cam, animated: true)
    }

    func zoom(vista: UIView, delta: Double) {
        guard let m = vista as? MLNMapView else { return }
        m.setZoomLevel(m.zoomLevel + delta, animated: true)
    }
}

/// Descarga offline de Loja (z10-16), igual que MapaOffline + MapaOfflineWorker en Android.
final class MapaOfflineMapLibre: NSObject, PuenteMapaOffline {
    private let estiloDescarga = MapaGacelasConst.shared.ESTILO_DESCARGA
    private var clave: Data { "\(MapaGacelasConst.shared.LOJA_ID)|\(estiloDescarga)".data(using: .utf8)! }
    private let monitor = NWPathMonitor()
    private var hayRed = true
    private var observador: NSKeyValueObservation?

    override init() {
        super.init()
        let nc = NotificationCenter.default
        nc.addObserver(self, selector: #selector(progreso(_:)), name: .MLNOfflinePackProgressChanged, object: nil)
        nc.addObserver(self, selector: #selector(errorPack(_:)), name: .MLNOfflinePackError, object: nil)
        nc.addObserver(self, selector: #selector(limite(_:)), name: .MLNOfflinePackMaximumMapboxTilesReached, object: nil)
        monitor.pathUpdateHandler = { [weak self] ruta in
            let antes = self?.hayRed ?? true
            self?.hayRed = ruta.status == .satisfied
            // Como el Worker de Android (red CONNECTED): con internet se descarga o reanuda sola.
            if ruta.status == .satisfied && !antes { DispatchQueue.main.async { self?.asegurar() } }
        }
        monitor.start(queue: DispatchQueue(label: "gacelas.red"))
    }

    func conRed() -> Bool { hayRed }

    private func propio() -> MLNOfflinePack? {
        MLNOfflineStorage.shared.packs?.first { $0.context == clave }
    }

    /// Los paquetes se cargan de forma asíncrona: espera a que MLNOfflineStorage los liste.
    private func conPaquetes(_ hacer: @escaping () -> Void) {
        if MLNOfflineStorage.shared.packs != nil { hacer(); return }
        observador = MLNOfflineStorage.shared.observe(\.packs, options: [.new]) { [weak self] s, _ in
            guard s.packs != nil else { return }
            self?.observador = nil
            DispatchQueue.main.async { hacer() }
        }
    }

    func asegurar() {
        MLNOfflineStorage.shared.setMaximumAmbientCacheSize(UInt(MapaGacelasConst.shared.CACHE_AMBIENTAL_BYTES)) { _ in }
        conPaquetes { [weak self] in
            guard let self else { return }
            // Paquetes viejos de Loja con otro estilo: se borran (como consultar() en Android).
            MLNOfflineStorage.shared.packs?.filter { p in
                (String(data: p.context, encoding: .utf8) ?? "").hasPrefix("\(MapaGacelasConst.shared.LOJA_ID)|") && p.context != self.clave
            }.forEach { MLNOfflineStorage.shared.removePack($0, withCompletionHandler: nil) }
            if let p = self.propio() {
                p.requestProgress()
                let completa = p.state == .complete
                if ReglasTeselas.shared.descargarSola(existe: true, completa: completa, wifi: self.hayRed) {
                    if MapaOfflineIos.shared.teselasGuardadas() == nil { self.guardarTeselas() }
                    MapaOfflineIos.shared.informar(tipo: 1, porcentaje: 0, megas: 0, mensaje: nil)
                    p.resume()
                } else { self.informar(p) }
            } else if ReglasTeselas.shared.descargarSola(existe: false, completa: false, wifi: self.hayRed) {
                self.descargar()
            } else {
                MapaOfflineIos.shared.informar(tipo: 0, porcentaje: 0, megas: 0, mensaje: nil)
            }
        }
    }

    func descargar() {
        guardarTeselas()
        guard let url = URL(string: estiloDescarga) else { return }
        let c = MapaGacelasConst.shared
        let limites = MLNCoordinateBounds(sw: CLLocationCoordinate2D(latitude: c.LOJA_SUR, longitude: c.LOJA_OESTE),
                                          ne: CLLocationCoordinate2D(latitude: c.LOJA_NORTE, longitude: c.LOJA_ESTE))
        let region = MLNTilePyramidOfflineRegion(styleURL: url, bounds: limites, fromZoomLevel: c.LOJA_ZOOM_MIN, toZoomLevel: c.LOJA_ZOOM_MAX)
        MapaOfflineIos.shared.informar(tipo: 1, porcentaje: 0, megas: 0, mensaje: nil)
        conPaquetes { [weak self] in
            guard let self else { return }
            if let p = self.propio() { p.resume(); return }
            MLNOfflineStorage.shared.addPack(for: region, withContext: self.clave) { pack, error in
                if let error { MapaOfflineIos.shared.informar(tipo: 3, porcentaje: 0, megas: 0, mensaje: error.localizedDescription); return }
                pack?.resume()
            }
        }
    }

    func eliminar() {
        conPaquetes { [weak self] in
            guard let self, let p = self.propio() else { return }
            p.suspend()
            MLNOfflineStorage.shared.removePack(p) { error in
                if let error { MapaOfflineIos.shared.informar(tipo: 3, porcentaje: 0, megas: 0, mensaje: error.localizedDescription) }
                else { MapaOfflineIos.shared.informar(tipo: 0, porcentaje: 0, megas: 0, mensaje: nil) }
            }
        }
    }

    /// Guarda la versión exacta de teselas de OpenFreeMap (TileJSON) para usar el mapa sin red.
    private func guardarTeselas() {
        guard let url = URL(string: MapaGacelasConst.shared.TILEJSON) else { return }
        var req = URLRequest(url: url)
        req.timeoutInterval = 8
        URLSession.shared.dataTask(with: req) { datos, _, _ in
            guard let datos, let txt = String(data: datos, encoding: .utf8) else { return }
            DispatchQueue.main.async { MapaOfflineIos.shared.guardarTileJson(tilejson: txt) }
        }.resume()
    }

    private func informar(_ p: MLNOfflinePack) {
        let pr = p.progress
        let megas = Double(pr.countOfBytesCompleted) / 1e6
        if p.state == .complete {
            MapaOfflineIos.shared.informar(tipo: 2, porcentaje: 100, megas: megas, mensaje: nil)
        } else if p.state == .active {
            let req = max(pr.countOfResourcesExpected, 1)
            MapaOfflineIos.shared.informar(tipo: 1, porcentaje: Int32(Double(pr.countOfResourcesCompleted) / Double(req) * 100), megas: megas, mensaje: nil)
        } else if MapaOfflineIos.shared.estadoActual() != 1 {
            MapaOfflineIos.shared.informar(tipo: 0, porcentaje: 0, megas: 0, mensaje: nil)
        }
    }

    @objc private func progreso(_ n: Notification) {
        guard let p = n.object as? MLNOfflinePack, p.context == clave else { return }
        DispatchQueue.main.async { self.informar(p) }
    }

    @objc private func errorPack(_ n: Notification) {
        guard let p = n.object as? MLNOfflinePack, p.context == clave else { return }
        let msj = hayRed ? ((n.userInfo?[MLNOfflinePackUserInfoKey.error] as? NSError)?.localizedDescription ?? "Error") : "Sin conexión: la descarga sigue al volver la red"
        DispatchQueue.main.async { MapaOfflineIos.shared.informar(tipo: 3, porcentaje: 0, megas: 0, mensaje: msj) }
    }

    @objc private func limite(_ n: Notification) {
        let l = (n.userInfo?[MLNOfflinePackUserInfoKey.maximumCount] as? NSNumber)?.int64Value ?? 0
        DispatchQueue.main.async { MapaOfflineIos.shared.informar(tipo: 3, porcentaje: 0, megas: 0, mensaje: "La zona supera el límite de \(l) teselas") }
    }
}
