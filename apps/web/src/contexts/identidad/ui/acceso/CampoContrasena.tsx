import { useState, type InputHTMLAttributes } from 'react'

/** Campo de contraseña con botón de ojo para mostrarla u ocultarla (objetivo táctil de 44 px). */
export function CampoContrasena({
  className = '',
  ...props
}: Omit<InputHTMLAttributes<HTMLInputElement>, 'type'>) {
  const [visible, setVisible] = useState(false)
  return (
    <div className="relative">
      <input {...props} type={visible ? 'text' : 'password'} className={`${className} pr-12`} />
      <button
        type="button"
        onClick={() => setVisible((v) => !v)}
        aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
        aria-pressed={visible}
        aria-controls={props.id}
        className="absolute right-0.5 top-1/2 -translate-y-1/2 w-11 h-11 flex items-center justify-center rounded-lg text-text-secondary hover:text-text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600"
      >
        <span className="material-symbols-outlined text-[22px]" aria-hidden="true">
          {visible ? 'visibility_off' : 'visibility'}
        </span>
      </button>
    </div>
  )
}
