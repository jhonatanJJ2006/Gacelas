import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

export function RecuperarContrasena() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitted(true);
  };

  if (submitted) {
    return (
      <div className="w-full min-h-screen bg-surface flex flex-col">
        <header className="w-full z-20 px-margin py-4">
          <button
            onClick={() => navigate('/inicio-sesion')}
            className="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-text-secondary hover:text-text-primary transition-colors"
          >
            <span className="material-symbols-outlined">arrow_back</span>
          </button>
        </header>

        <main className="flex-1 px-6 pb-8 flex flex-col">
          <div className="flex-1 flex flex-col justify-center items-center max-w-md mx-auto w-full text-center">
            <div className="w-16 h-16 rounded-full bg-secondary-600/20 flex items-center justify-center mb-6">
              <span className="material-symbols-outlined text-[32px] text-secondary-500">mark_email_read</span>
            </div>

            <h1 className="text-text-primary font-headline-lg text-2xl font-bold mb-3">
              Revisa tu correo
            </h1>
            <p className="text-text-secondary text-sm mb-8">
              Te hemos enviado un enlace para restablecer tu contraseña a <span className="text-text-primary font-medium">{email}</span>
            </p>

            <button
              onClick={() => navigate('/inicio-sesion')}
              className="w-full h-14 bg-primary-600 hover:bg-primary-700 active:scale-[0.98] text-bg-primary font-headline-md font-bold text-base rounded-xl flex items-center justify-center gap-2 shadow-[0_8px_20px_-4px_rgba(217,119,6,0.35)] transition-all cursor-pointer"
            >
              Volver al inicio de sesión
            </button>
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="w-full min-h-screen bg-surface flex flex-col">
      <header className="w-full z-20 px-margin py-4">
        <button
          onClick={() => navigate('/inicio-sesion')}
          className="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-text-secondary hover:text-text-primary transition-colors"
        >
          <span className="material-symbols-outlined">arrow_back</span>
        </button>
      </header>

      <main className="flex-1 px-6 pb-8 flex flex-col">
        <div className="flex-1 flex flex-col justify-center max-w-md mx-auto w-full">
          <div className="mb-8">
            <div className="w-12 h-12 rounded-full bg-primary-600/20 flex items-center justify-center mb-6">
              <span className="material-symbols-outlined text-[24px] text-primary-500">lock_reset</span>
            </div>

            <h1 className="text-text-primary font-headline-lg text-3xl font-bold mb-2">
              Recuperar contraseña
            </h1>
            <p className="text-text-secondary text-sm">
              Ingresa tu correo electrónico y te enviaremos un enlace para restablecer tu contraseña
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-text-primary mb-2">
                Correo electrónico
              </label>
              <input
                type="email"
                id="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full h-12 px-4 bg-surface-container border border-border-subtle rounded-lg text-text-primary placeholder-text-secondary focus:outline-none focus:ring-2 focus:ring-primary-600 focus:border-transparent"
                placeholder="tu@email.com"
                required
              />
            </div>

            <button
              type="submit"
              className="w-full h-14 bg-primary-600 hover:bg-primary-700 active:scale-[0.98] text-bg-primary font-headline-md font-bold text-base rounded-xl flex items-center justify-center gap-2 shadow-[0_8px_20px_-4px_rgba(217,119,6,0.35)] transition-all cursor-pointer"
            >
              <span>Enviar enlace</span>
              <span className="material-symbols-outlined text-[20px]">arrow_forward</span>
            </button>
          </form>
        </div>
      </main>
    </div>
  );
}
