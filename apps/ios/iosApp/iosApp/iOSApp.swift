import SwiftUI
import GoogleSignIn
import Shared

@main
struct iOSApp: App {
    /// Mapa MapLibre y descarga offline de Loja (puente con Shared), antes de la primera pantalla.
    private static let offline = MapaOfflineMapLibre()
    private static let acceso = AccesoNativo()
    init() {
        PuenteMapaKt.registrarMapaIos(puente: PuenteMapLibre(), offline: Self.offline)
        AccesoIosKt.registrarAccesoIos(puente: Self.acceso)
        Runner3DIosKt.registrarCorredor3DIos(puente: Corredor3DSceneKit())
    }

    var body: some Scene {
        WindowGroup { ContentView().ignoresSafeArea().onOpenURL { GIDSignIn.sharedInstance.handle($0) } }
    }
}

struct ContentView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ vc: UIViewController, context: Context) {}
}
