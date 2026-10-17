import SceneKit
import GLTFKit2
import Shared
import UIKit

/// Corredor 3D con SceneKit + GLTFKit2: el mismo GLB, clips, escala y cámara que Runner3D (SceneView/Filament)
/// en Android. Se renderiza fuera de pantalla (SCNRenderer) y Compose dibuja cada cuadro (Runner3DIos.kt).
final class Corredor3DSceneKit: NSObject, PuenteCorredor3D {
    /// El GLB se lee una vez (3,6 MB) y cada corredor clona su escena.
    private static let asset: GLTFAsset? = {
        guard let url = Bundle.main.url(forResource: "gacelas-runner-animated", withExtension: "glb") else { return nil }
        return try? GLTFAsset(url: url, options: [:])
    }()

    func crear(clip: String, alturaY: Double, camaraX: Double, camaraY: Double, camaraZ: Double) -> Corredor3DNativo? {
        guard let asset = Self.asset else { return nil }
        return CorredorRenderizado(asset: asset, clip: clip, alturaY: alturaY, camara: SCNVector3(camaraX, camaraY, camaraZ))
    }
}

private final class CorredorRenderizado: NSObject, Corredor3DNativo {
    private let renderer: SCNRenderer
    private let modelo: SCNNode

    init?(asset: GLTFAsset, clip: String, alturaY: Double, camara: SCNVector3) {
        let fuente = GLTFSCNSceneSource(asset: asset)
        guard let base = fuente.defaultScene, let device = MTLCreateSystemDefaultDevice() else { return nil }
        let modelo = SCNNode()
        let escena = SCNScene()
        escena.background.contents = UIColor.clear
        // Mismo esquema que ModelNode(scaleToUnits = 1.8, centerOrigin = 0) + position.y = alturaY del clip.
        base.rootNode.childNodes.forEach { modelo.addChildNode($0) }
        let (minimo, maximo) = modelo.boundingBox
        let lado = max(maximo.x - minimo.x, maximo.y - minimo.y, maximo.z - minimo.z)
        if lado > 0 { let s = 1.8 / lado; modelo.scale = SCNVector3(s, s, s) }
        modelo.position = SCNVector3(0, Float(alturaY), 0)
        escena.rootNode.addChildNode(modelo)
        if let animacion = fuente.animations.first(where: { $0.name == clip }) ?? fuente.animations.first {
            let jugador = animacion.animationPlayer
            jugador.animation.usesSceneTimeBase = true
            jugador.animation.repeatCount = .greatestFiniteMagnitude
            modelo.addAnimationPlayer(jugador, forKey: clip)
            jugador.play()
        }
        // Cámara como CameraNode de SceneView: lente de 28 mm (sensor de 24 mm) mirando al origen.
        let ojo = SCNNode()
        ojo.camera = SCNCamera()
        ojo.camera?.focalLength = 28
        ojo.camera?.sensorHeight = 24
        ojo.camera?.zNear = 0.05
        ojo.camera?.zFar = 100
        ojo.position = camara
        ojo.look(at: SCNVector3(0, 0, 0))
        escena.rootNode.addChildNode(ojo)
        // Luz principal + ambiente (Filament usa una luz direccional y un IBL por defecto).
        let sol = SCNNode()
        sol.light = SCNLight()
        sol.light?.type = .directional
        sol.light?.intensity = 1200
        sol.eulerAngles = SCNVector3(-Float.pi / 4, Float.pi / 6, 0)
        escena.rootNode.addChildNode(sol)
        let ambiente = SCNNode()
        ambiente.light = SCNLight()
        ambiente.light?.type = .ambient
        ambiente.light?.intensity = 500
        escena.rootNode.addChildNode(ambiente)
        escena.lightingEnvironment.contents = UIColor(white: 0.55, alpha: 1)
        let r = SCNRenderer(device: device, options: nil)
        r.scene = escena
        r.pointOfView = ojo
        self.renderer = r
        self.modelo = modelo
        super.init()
    }

    func cuadro(tiempo: Double, rotacionY: Double, ancho: Int32, alto: Int32) -> Data? {
        guard ancho > 0, alto > 0 else { return nil }
        modelo.eulerAngles = SCNVector3(0, Float(rotacionY * .pi / 180), 0)
        let w = Int(ancho), h = Int(alto)
        let imagen = renderer.snapshot(atTime: tiempo, with: CGSize(width: w, height: h), antialiasingMode: .multisampling4X)
        guard let cg = imagen.cgImage else { return nil }
        var datos = Data(count: w * h * 4)
        let ok = datos.withUnsafeMutableBytes { (p: UnsafeMutableRawBufferPointer) -> Bool in
            guard let ctx = CGContext(data: p.baseAddress, width: w, height: h, bitsPerComponent: 8, bytesPerRow: w * 4,
                                      space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue) else { return false }
            ctx.clear(CGRect(x: 0, y: 0, width: w, height: h))
            ctx.draw(cg, in: CGRect(x: 0, y: 0, width: w, height: h))
            return true
        }
        return ok ? datos : nil
    }
}
