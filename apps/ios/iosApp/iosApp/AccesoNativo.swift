import AuthenticationServices
import GoogleSignIn
import Shared
import UIKit

private func ventanaActiva() -> UIViewController? {
    let escenas = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
    var vc = escenas.flatMap { $0.windows }.first { $0.isKeyWindow }?.rootViewController
    while let p = vc?.presentedViewController { vc = p }
    return vc
}

private func clave(_ n: String) -> String {
    let v = (Bundle.main.object(forInfoDictionaryKey: n) as? String ?? "").trimmingCharacters(in: .whitespaces)
    return v.hasPrefix("$(") ? "" : v
}

/// Google Sign-In y Sign in with Apple nativos; el ID token y el nonce van a Supabase desde Kotlin (SesionIos).
final class AccesoNativo: NSObject, PuenteAcceso, ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {
    private var alApple: AlTokenAcceso?
    private var botones: [ObjectIdentifier: AlTocarBoton] = [:]

    func googleConfigurado() -> Bool { !clave("GIDClientID").isEmpty }

    func google(nonceSha256: String, alTerminar: AlTokenAcceso) {
        let id = clave("GIDClientID")
        guard !id.isEmpty, let vc = ventanaActiva() else {
            alTerminar.error(mensaje: "El inicio con Google aún no está configurado en esta versión de la app"); return
        }
        let web = clave("GIDServerClientID")
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: id, serverClientID: web.isEmpty ? nil : web)
        GIDSignIn.sharedInstance.signIn(withPresenting: vc, hint: nil, additionalScopes: nil, nonce: nonceSha256) { r, error in
            if let e = error as NSError? {
                alTerminar.error(mensaje: e.code == GIDSignInError.canceled.rawValue ? nil : "No se pudo usar Google (\(e.localizedDescription))")
                return
            }
            guard let token = r?.user.idToken?.tokenString else { alTerminar.error(mensaje: "Google no devolvió una cuenta válida"); return }
            alTerminar.ok(idToken: token)
        }
    }

    func apple(nonceSha256: String, alTerminar: AlTokenAcceso) {
        alApple = alTerminar
        let req = ASAuthorizationAppleIDProvider().createRequest()
        req.requestedScopes = [.fullName, .email]
        req.nonce = nonceSha256
        let c = ASAuthorizationController(authorizationRequests: [req])
        c.delegate = self
        c.presentationContextProvider = self
        c.performRequests()
    }

    func botonApple(alTocar: AlTocarBoton) -> UIView {
        let b = ASAuthorizationAppleIDButton(authorizationButtonType: .continue, authorizationButtonStyle: .white)
        b.cornerRadius = 12
        botones[ObjectIdentifier(b)] = alTocar
        b.addTarget(self, action: #selector(tocoApple(_:)), for: .touchUpInside)
        return b
    }

    @objc private func tocoApple(_ b: UIControl) { botones[ObjectIdentifier(b)]?.tocar() }

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        ventanaActiva()?.view.window ?? ASPresentationAnchor()
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        guard let c = authorization.credential as? ASAuthorizationAppleIDCredential,
              let datos = c.identityToken, let token = String(data: datos, encoding: .utf8) else {
            alApple?.error(mensaje: "Apple no devolvió una cuenta válida"); alApple = nil; return
        }
        alApple?.ok(idToken: token); alApple = nil
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        let cancelado = (error as? ASAuthorizationError)?.code == .canceled
        alApple?.error(mensaje: cancelado ? nil : "No se pudo usar Apple (\(error.localizedDescription))"); alApple = nil
    }
}
