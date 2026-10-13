import SwiftUI
import Shared

@main
struct iOSApp: App {
    /// Mapa MapLibre y descarga offline de Loja (puente con Shared), antes de la primera pantalla.
    private static let offline = MapaOfflineMapLibre()
    init() { PuenteMapaKt.registrarMapaIos(puente: PuenteMapLibre(), offline: Self.offline) }

    var body: some Scene {
        WindowGroup { ContentView().ignoresSafeArea() }
    }
}

struct ContentView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ vc: UIViewController, context: Context) {}
}
