import { useNavigate } from 'react-router-dom'

export function Feed() {
  const navigate = useNavigate()
  const posts = [
    {
      id: 1,
      user: 'María López',
      time: 'hace 2h',
      distance: 5.2,
      duration: '28:34',
      pace: '5:30',
      likes: 23,
      comments: 5,
    },
    {
      id: 2,
      user: 'Carlos Ruiz',
      time: 'hace 4h',
      distance: 8.5,
      duration: '45:20',
      pace: '5:20',
      likes: 18,
      comments: 3,
    },
  ]

  return (
    <div className="flex flex-col w-full">
      <div className="px-margin pt-space-md pb-space-sm">
        <div className="flex items-center justify-between mb-4">
          <h1 className="font-headline-md text-headline-md text-text-primary">Comunidad</h1>
          <button className="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-text-secondary hover:text-text-primary transition-colors">
            <span className="material-symbols-outlined">filter_list</span>
          </button>
        </div>

        <div className="flex gap-2 mb-4">
          <button className="px-4 py-2 rounded-full font-label-md text-label-md bg-primary-600 text-bg-primary">
            Feed
          </button>
          <button
            onClick={() => navigate('/comunidad/eventos')}
            className="px-4 py-2 rounded-full font-label-md text-label-md bg-surface-container text-text-secondary hover:text-text-primary"
          >
            Eventos
          </button>
        </div>
      </div>

      <div className="px-margin space-y-4 pb-space-3xl">
        {posts.map((post) => (
          <div
            key={post.id}
            className="bg-surface-container rounded-2xl border border-border-subtle overflow-hidden"
          >
            <div className="p-4 flex items-center gap-3 border-b border-border-subtle">
              <div className="w-10 h-10 rounded-full bg-surface-variant"></div>
              <div className="flex-1">
                <p className="font-body-md text-body-md text-text-primary font-semibold">
                  {post.user}
                </p>
                <p className="font-body-sm text-body-sm text-text-secondary">{post.time}</p>
              </div>
              <button className="text-text-secondary hover:text-text-primary">
                <span className="material-symbols-outlined">more_vert</span>
              </button>
            </div>

            <div className="h-48 bg-gradient-to-br from-primary-600/20 to-secondary-600/20 flex items-center justify-center">
              <span className="material-symbols-outlined text-[64px] text-primary">route</span>
            </div>

            <div className="p-4">
              <div className="grid grid-cols-3 gap-4 mb-4">
                <div className="text-center">
                  <div className="font-headline-sm text-headline-sm text-primary">
                    {post.distance}
                  </div>
                  <div className="font-label-md text-label-md text-text-secondary">km</div>
                </div>
                <div className="text-center">
                  <div className="font-headline-sm text-headline-sm text-text-primary">
                    {post.duration}
                  </div>
                  <div className="font-label-md text-label-md text-text-secondary">tiempo</div>
                </div>
                <div className="text-center">
                  <div className="font-headline-sm text-headline-sm text-secondary">
                    {post.pace}
                  </div>
                  <div className="font-label-md text-label-md text-text-secondary">/km</div>
                </div>
              </div>

              <div className="flex items-center gap-6 pt-3 border-t border-border-subtle">
                <button className="flex items-center gap-2 text-text-secondary hover:text-primary transition-colors">
                  <span className="material-symbols-outlined">favorite_border</span>
                  <span className="font-label-md text-label-md">{post.likes}</span>
                </button>
                <button className="flex items-center gap-2 text-text-secondary hover:text-text-primary transition-colors">
                  <span className="material-symbols-outlined">chat_bubble_outline</span>
                  <span className="font-label-md text-label-md">{post.comments}</span>
                </button>
                <button className="flex items-center gap-2 text-text-secondary hover:text-text-primary transition-colors ml-auto">
                  <span className="material-symbols-outlined">share</span>
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
