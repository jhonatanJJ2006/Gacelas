/** Generado con `supabase gen types typescript --schema public` tras la migracion 033. No editar a mano. */

export type Json = string | number | boolean | null | { [key: string]: Json | undefined } | Json[]

export type Database = {
  public: {
    Tables: {
      activities: {
        Row: {
          activity_type: Database['public']['Enums']['activity_type'] | null
          avg_pace_seconds_per_km: number | null
          client_id: string | null
          comments_count: number | null
          created_at: string | null
          description: string | null
          distance_meters: number | null
          duration_seconds: number | null
          elevation_gain_meters: number | null
          end_time: string | null
          ended_location: unknown
          id: string
          is_live_shared: boolean | null
          is_public: boolean | null
          likes_count: number | null
          public_points: NonNullable<Json>
          reposts_count: number
          splits: NonNullable<Json>
          start_time: string
          started_location: unknown
          title: string
          updated_at: string | null
          user_id: string
        }
        ComputedFields: never
        Insert: {
          activity_type?: Database['public']['Enums']['activity_type'] | null
          avg_pace_seconds_per_km?: number | null
          client_id?: string | null
          comments_count?: number | null
          created_at?: string | null
          description?: string | null
          distance_meters?: number | null
          duration_seconds?: number | null
          elevation_gain_meters?: number | null
          end_time?: string | null
          ended_location?: unknown
          id?: string
          is_live_shared?: boolean | null
          is_public?: boolean | null
          likes_count?: number | null
          public_points?: NonNullable<Json>
          reposts_count?: number
          splits?: NonNullable<Json>
          start_time: string
          started_location?: unknown
          title: string
          updated_at?: string | null
          user_id: string
        }
        Update: {
          activity_type?: Database['public']['Enums']['activity_type'] | null
          avg_pace_seconds_per_km?: number | null
          client_id?: string | null
          comments_count?: number | null
          created_at?: string | null
          description?: string | null
          distance_meters?: number | null
          duration_seconds?: number | null
          elevation_gain_meters?: number | null
          end_time?: string | null
          ended_location?: unknown
          id?: string
          is_live_shared?: boolean | null
          is_public?: boolean | null
          likes_count?: number | null
          public_points?: NonNullable<Json>
          reposts_count?: number
          splits?: NonNullable<Json>
          start_time?: string
          started_location?: unknown
          title?: string
          updated_at?: string | null
          user_id?: string
        }
        Relationships: []
      }
      club_posts: {
        Row: {
          body: string
          club_id: string
          created_at: string
          id: string
          user_id: string
        }
        ComputedFields: never
        Insert: {
          body: string
          club_id: string
          created_at?: string
          id?: string
          user_id: string
        }
        Update: {
          body?: string
          club_id?: string
          created_at?: string
          id?: string
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: 'club_posts_club_id_fkey'
            columns: ['club_id']
            isOneToOne: false
            referencedRelation: 'clubs'
            referencedColumns: ['id']
          },
        ]
      }
      follows: {
        Row: {
          created_at: string | null
          follower_id: string
          following_id: string
        }
        ComputedFields: never
        Insert: {
          created_at?: string | null
          follower_id: string
          following_id: string
        }
        Update: {
          created_at?: string | null
          follower_id?: string
          following_id?: string
        }
        Relationships: []
      }
      live_sessions: {
        Row: {
          audience: string
          distance_m: number
          elapsed_s: number
          ended_at: string | null
          event_id: string | null
          hidden: boolean
          id: string
          last_at: string | null
          last_lat: number | null
          last_lng: number | null
          points: NonNullable<Json>
          started_at: string
          user_id: string
        }
        ComputedFields: never
        Insert: {
          audience?: string
          distance_m?: number
          elapsed_s?: number
          ended_at?: string | null
          event_id?: string | null
          hidden?: boolean
          id?: string
          last_at?: string | null
          last_lat?: number | null
          last_lng?: number | null
          points?: NonNullable<Json>
          started_at?: string
          user_id: string
        }
        Update: {
          audience?: string
          distance_m?: number
          elapsed_s?: number
          ended_at?: string | null
          event_id?: string | null
          hidden?: boolean
          id?: string
          last_at?: string | null
          last_lat?: number | null
          last_lng?: number | null
          points?: NonNullable<Json>
          started_at?: string
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: 'live_sessions_event_id_fkey'
            columns: ['event_id']
            isOneToOne: false
            referencedRelation: 'events'
            referencedColumns: ['id']
          },
        ]
      }
      post_comments: {
        Row: {
          body: string
          created_at: string | null
          id: string
          post_id: string
          user_id: string
        }
        ComputedFields: never
        Insert: {
          body: string
          created_at?: string | null
          id?: string
          post_id: string
          user_id: string
        }
        Update: {
          body?: string
          created_at?: string | null
          id?: string
          post_id?: string
          user_id?: string
        }
        Relationships: []
      }
      post_likes: {
        Row: {
          created_at: string | null
          post_id: string
          user_id: string
        }
        ComputedFields: never
        Insert: {
          created_at?: string | null
          post_id: string
          user_id: string
        }
        Update: {
          created_at?: string | null
          post_id?: string
          user_id?: string
        }
        Relationships: []
      }
      post_reposts: {
        Row: {
          activity_id: string
          created_at: string
          user_id: string
        }
        ComputedFields: never
        Insert: {
          activity_id: string
          created_at?: string
          user_id: string
        }
        Update: {
          activity_id?: string
          created_at?: string
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: 'post_reposts_activity_id_fkey'
            columns: ['activity_id']
            isOneToOne: false
            referencedRelation: 'activities'
            referencedColumns: ['id']
          },
        ]
      }
      runner_positions: {
        Row: {
          lat: number
          lng: number
          running: boolean
          updated_at: string
          user_id: string
        }
        ComputedFields: never
        Insert: {
          lat: number
          lng: number
          running?: boolean
          updated_at?: string
          user_id: string
        }
        Update: {
          lat?: number
          lng?: number
          running?: boolean
          updated_at?: string
          user_id?: string
        }
        Relationships: []
      }
      spatial_ref_sys: {
        Row: {
          auth_name: string | null
          auth_srid: number | null
          proj4text: string | null
          srid: number
          srtext: string | null
        }
        ComputedFields: never
        Insert: {
          auth_name?: string | null
          auth_srid?: number | null
          proj4text?: string | null
          srid: number
          srtext?: string | null
        }
        Update: {
          auth_name?: string | null
          auth_srid?: number | null
          proj4text?: string | null
          srid?: number
          srtext?: string | null
        }
        Relationships: []
      }
    }
    Views: {
      activity_summaries: {
        Row: {
          activity_id: string
          distance_meters: number | null
          duration_seconds: number | null
          point_count: number
          updated_at: string
        }
        ComputedFields: never
        Insert: {
          activity_id?: string | null
          distance_meters?: number | null
          duration_seconds?: number | null
          point_count?: number | null
          updated_at?: string | null
        }
        Update: {
          activity_id?: string | null
          distance_meters?: number | null
          duration_seconds?: number | null
          point_count?: number | null
          updated_at?: string | null
        }
        Relationships: []
      }
      activity_tracks: {
        Row: {
          activity_id: string
          points: NonNullable<Json>
          user_id: string
        }
        ComputedFields: never
        Insert: {
          activity_id?: string | null
          points?: Json | null
          user_id?: string | null
        }
        Update: {
          activity_id?: string | null
          points?: Json | null
          user_id?: string | null
        }
        Relationships: []
      }
      app_settings: {
        Row: {
          key: string
          updated_at: string
          value: NonNullable<Json>
        }
        ComputedFields: never
        Insert: {
          key?: string | null
          updated_at?: string | null
          value?: Json | null
        }
        Update: {
          key?: string | null
          updated_at?: string | null
          value?: Json | null
        }
        Relationships: []
      }
      club_members: {
        Row: {
          club_id: string
          created_at: string
          role: string
          status: string
          user_id: string
        }
        ComputedFields: never
        Insert: {
          club_id?: string | null
          created_at?: string | null
          role?: string | null
          status?: string | null
          user_id?: string | null
        }
        Update: {
          club_id?: string | null
          created_at?: string | null
          role?: string | null
          status?: string | null
          user_id?: string | null
        }
        Relationships: []
      }
      clubs: {
        Row: {
          created_at: string
          description: string
          id: string
          location: string
          members_count: number
          name: string
          owner_id: string
          requires_approval: boolean
        }
        ComputedFields: never
        Insert: {
          created_at?: string | null
          description?: string | null
          id?: string | null
          location?: string | null
          members_count?: number | null
          name?: string | null
          owner_id?: string | null
          requires_approval?: boolean | null
        }
        Update: {
          created_at?: string | null
          description?: string | null
          id?: string | null
          location?: string | null
          members_count?: number | null
          name?: string | null
          owner_id?: string | null
          requires_approval?: boolean | null
        }
        Relationships: []
      }
      consents: {
        Row: {
          created_at: string
          granted: boolean
          id: string
          kind: string
          user_id: string
          version: string
        }
        ComputedFields: never
        Insert: {
          created_at?: string | null
          granted?: boolean | null
          id?: string | null
          kind?: string | null
          user_id?: string | null
          version?: string | null
        }
        Update: {
          created_at?: string | null
          granted?: boolean | null
          id?: string | null
          kind?: string | null
          user_id?: string | null
          version?: string | null
        }
        Relationships: []
      }
      content_report_actions: {
        Row: {
          action: string
          created_at: string
          id: string
          moderator_id: string | null
          note: string
          report_id: string
        }
        ComputedFields: never
        Insert: {
          action?: string | null
          created_at?: string | null
          id?: string | null
          moderator_id?: string | null
          note?: string | null
          report_id?: string | null
        }
        Update: {
          action?: string | null
          created_at?: string | null
          id?: string | null
          moderator_id?: string | null
          note?: string | null
          report_id?: string | null
        }
        Relationships: []
      }
      content_reports: {
        Row: {
          assigned_to: string | null
          created_at: string
          detail: string
          id: string
          legacy_post_report_id: string | null
          reason: string
          reporter_id: string
          resolution_note: string
          resolved_at: string | null
          resolved_by: string | null
          status: string
          target_id: string
          target_type: string
          updated_at: string
        }
        ComputedFields: never
        Insert: {
          assigned_to?: string | null
          created_at?: string | null
          detail?: string | null
          id?: string | null
          legacy_post_report_id?: string | null
          reason?: string | null
          reporter_id?: string | null
          resolution_note?: string | null
          resolved_at?: string | null
          resolved_by?: string | null
          status?: string | null
          target_id?: string | null
          target_type?: string | null
          updated_at?: string | null
        }
        Update: {
          assigned_to?: string | null
          created_at?: string | null
          detail?: string | null
          id?: string | null
          legacy_post_report_id?: string | null
          reason?: string | null
          reporter_id?: string | null
          resolution_note?: string | null
          resolved_at?: string | null
          resolved_by?: string | null
          status?: string | null
          target_id?: string | null
          target_type?: string | null
          updated_at?: string | null
        }
        Relationships: []
      }
      event_reminders: {
        Row: {
          event_id: string
          kind: string
          user_id: string
        }
        ComputedFields: never
        Insert: {
          event_id?: string | null
          kind?: string | null
          user_id?: string | null
        }
        Update: {
          event_id?: string | null
          kind?: string | null
          user_id?: string | null
        }
        Relationships: []
      }
      event_rsvps: {
        Row: {
          created_at: string | null
          event_id: string
          status: string
          user_id: string
        }
        ComputedFields: never
        Insert: {
          created_at?: string | null
          event_id?: string | null
          status?: string | null
          user_id?: string | null
        }
        Update: {
          created_at?: string | null
          event_id?: string | null
          status?: string | null
          user_id?: string | null
        }
        Relationships: []
      }
      events: {
        Row: {
          base_participants: number
          club_id: string | null
          created_at: string | null
          created_by: string | null
          date_label: string
          description: string
          distance_km: number
          id: string
          kind: string
          location: string
          max_participants: number | null
          meeting_lat: number | null
          meeting_lng: number | null
          published: boolean
          route_id: string | null
          starts_at: string | null
          time_label: string
          title: string
        }
        ComputedFields: never
        Insert: {
          base_participants?: number | null
          club_id?: string | null
          created_at?: string | null
          created_by?: string | null
          date_label?: string | null
          description?: string | null
          distance_km?: number | null
          id?: string | null
          kind?: string | null
          location?: string | null
          max_participants?: number | null
          meeting_lat?: number | null
          meeting_lng?: number | null
          published?: boolean | null
          route_id?: string | null
          starts_at?: string | null
          time_label?: string | null
          title?: string | null
        }
        Update: {
          base_participants?: number | null
          club_id?: string | null
          created_at?: string | null
          created_by?: string | null
          date_label?: string | null
          description?: string | null
          distance_km?: number | null
          id?: string | null
          kind?: string | null
          location?: string | null
          max_participants?: number | null
          meeting_lat?: number | null
          meeting_lng?: number | null
          published?: boolean | null
          route_id?: string | null
          starts_at?: string | null
          time_label?: string | null
          title?: string | null
        }
        Relationships: []
      }
      geography_columns: {
        Row: {
          coord_dimension: number | null
          f_geography_column: unknown
          f_table_catalog: unknown
          f_table_name: unknown
          f_table_schema: unknown
          srid: number | null
          type: string | null
        }
        ComputedFields: never
        Relationships: []
      }
      geometry_columns: {
        Row: {
          coord_dimension: number | null
          f_geometry_column: unknown
          f_table_catalog: string | null
          f_table_name: unknown
          f_table_schema: unknown
          srid: number | null
          type: string | null
        }
        ComputedFields: never
        Insert: {
          coord_dimension?: number | null
          f_geometry_column?: unknown
          f_table_catalog?: string | null
          f_table_name?: unknown
          f_table_schema?: unknown
          srid?: number | null
          type?: string | null
        }
        Update: {
          coord_dimension?: number | null
          f_geometry_column?: unknown
          f_table_catalog?: string | null
          f_table_name?: unknown
          f_table_schema?: unknown
          srid?: number | null
          type?: string | null
        }
        Relationships: []
      }
      importaciones_ecu911: {
        Row: {
          archivo: string
          estado: string
          fecha: string
          filas: number
          filas_loja: number
          id: number
          mensaje: string | null
          periodo: string
        }
        ComputedFields: never
        Insert: {
          archivo?: string | null
          estado?: string | null
          fecha?: string | null
          filas?: number | null
          filas_loja?: number | null
          id?: number | null
          mensaje?: string | null
          periodo?: string | null
        }
        Update: {
          archivo?: string | null
          estado?: string | null
          fecha?: string | null
          filas?: number | null
          filas_loja?: number | null
          id?: number | null
          mensaje?: string | null
          periodo?: string | null
        }
        Relationships: []
      }
      incidentes_agregados: {
        Row: {
          anio: number
          cod_parroquia: string
          conteo: number
          dia_semana: number
          fecha_carga: string
          fuente: string
          id: number
          mes: number
          servicio: string
          subtipo: string
        }
        ComputedFields: never
        Insert: {
          anio?: number | null
          cod_parroquia?: string | null
          conteo?: number | null
          dia_semana?: number | null
          fecha_carga?: string | null
          fuente?: string | null
          id?: number | null
          mes?: number | null
          servicio?: string | null
          subtipo?: string | null
        }
        Update: {
          anio?: number | null
          cod_parroquia?: string | null
          conteo?: number | null
          dia_semana?: number | null
          fecha_carga?: string | null
          fuente?: string | null
          id?: number | null
          mes?: number | null
          servicio?: string | null
          subtipo?: string | null
        }
        Relationships: []
      }
      incidentes_confirmaciones: {
        Row: {
          created_at: string
          incidente_id: string
          user_id: string
        }
        ComputedFields: never
        Insert: {
          created_at?: string | null
          incidente_id?: string | null
          user_id?: string | null
        }
        Update: {
          created_at?: string | null
          incidente_id?: string | null
          user_id?: string | null
        }
        Relationships: []
      }
      incidentes_publicos: {
        Row: {
          confirmaciones: number
          descripcion: string | null
          estado: string
          fotos: string[]
          id: string
          lat: number | null
          lng: number | null
          ocurrido_hora: string | null
          tipo: string
        }
        ComputedFields: never
        Insert: {
          confirmaciones?: number | null
          descripcion?: never
          estado?: string | null
          fotos?: string[] | null
          id?: string | null
          lat?: never
          lng?: never
          ocurrido_hora?: never
          tipo?: string | null
        }
        Update: {
          confirmaciones?: number | null
          descripcion?: never
          estado?: string | null
          fotos?: string[] | null
          id?: string | null
          lat?: never
          lng?: never
          ocurrido_hora?: never
          tipo?: string | null
        }
        Relationships: []
      }
      incidentes_usuario: {
        Row: {
          confirmaciones: number
          created_at: string
          descripcion: string | null
          direccion: string | null
          estado: string
          fotos: string[]
          geom: unknown
          id: string
          ocurrido_at: string
          tipo: string
          user_id: string
        }
        ComputedFields: never
        Insert: {
          confirmaciones?: number | null
          created_at?: string | null
          descripcion?: string | null
          direccion?: string | null
          estado?: string | null
          fotos?: string[] | null
          geom?: unknown
          id?: string | null
          ocurrido_at?: string | null
          tipo?: string | null
          user_id?: string | null
        }
        Update: {
          confirmaciones?: number | null
          created_at?: string | null
          descripcion?: string | null
          direccion?: string | null
          estado?: string | null
          fotos?: string[] | null
          geom?: unknown
          id?: string | null
          ocurrido_at?: string | null
          tipo?: string | null
          user_id?: string | null
        }
        Relationships: []
      }
      legal_versions: {
        Row: {
          kind: string
          published_at: string
          version: string
        }
        ComputedFields: never
        Insert: {
          kind?: string | null
          published_at?: string | null
          version?: string | null
        }
        Update: {
          kind?: string | null
          published_at?: string | null
          version?: string | null
        }
        Relationships: []
      }
      live_private: {
        Row: {
          session_id: string
          start_lat: number
          start_lng: number
        }
        ComputedFields: never
        Insert: {
          session_id?: string | null
          start_lat?: number | null
          start_lng?: number | null
        }
        Update: {
          session_id?: string | null
          start_lat?: number | null
          start_lng?: number | null
        }
        Relationships: []
      }
      live_sharing_settings: {
        Row: {
          audience: string
          enabled: boolean
          notify_followers: boolean
          updated_at: string
          user_id: string
        }
        ComputedFields: never
        Insert: {
          audience?: string | null
          enabled?: boolean | null
          notify_followers?: boolean | null
          updated_at?: string | null
          user_id?: string | null
        }
        Update: {
          audience?: string | null
          enabled?: boolean | null
          notify_followers?: boolean | null
          updated_at?: string | null
          user_id?: string | null
        }
        Relationships: []
      }
      notifications: {
        Row: {
          actor_name: string
          created_at: string | null
          id: string
          message: string
          read: boolean
          type: string
          user_id: string
        }
        ComputedFields: never
        Insert: {
          actor_name?: string | null
          created_at?: string | null
          id?: string | null
          message?: string | null
          read?: boolean | null
          type?: string | null
          user_id?: string | null
        }
        Update: {
          actor_name?: string | null
          created_at?: string | null
          id?: string | null
          message?: string | null
          read?: boolean | null
          type?: string | null
          user_id?: string | null
        }
        Relationships: []
      }
      parroquias: {
        Row: {
          canton: string
          cod_parroquia: string
          geom: unknown
          nombre: string
          poblacion: number | null
          provincia: string
        }
        ComputedFields: never
        Insert: {
          canton?: string | null
          cod_parroquia?: string | null
          geom?: unknown
          nombre?: string | null
          poblacion?: number | null
          provincia?: string | null
        }
        Update: {
          canton?: string | null
          cod_parroquia?: string | null
          geom?: unknown
          nombre?: string | null
          poblacion?: number | null
          provincia?: string | null
        }
        Relationships: []
      }
      pg_all_foreign_keys: {
        Row: {
          fk_columns: unknown[] | null
          fk_constraint_name: unknown
          fk_schema_name: unknown
          fk_table_name: unknown
          fk_table_oid: unknown
          is_deferrable: boolean | null
          is_deferred: boolean | null
          match_type: string | null
          on_delete: string | null
          on_update: string | null
          pk_columns: unknown[] | null
          pk_constraint_name: unknown
          pk_index_name: unknown
          pk_schema_name: unknown
          pk_table_name: unknown
          pk_table_oid: unknown
        }
        ComputedFields: never
        Relationships: []
      }
      post_images: {
        Row: {
          activity_id: string
          created_at: string
          id: string
          path: string
          position: number
          user_id: string
        }
        ComputedFields: never
        Insert: {
          activity_id?: string | null
          created_at?: string | null
          id?: string | null
          path?: string | null
          position?: number | null
          user_id?: string | null
        }
        Update: {
          activity_id?: string | null
          created_at?: string | null
          id?: string | null
          path?: string | null
          position?: number | null
          user_id?: string | null
        }
        Relationships: []
      }
      post_reports: {
        Row: {
          created_at: string
          detail: string
          id: string
          post_id: string
          reason: string
          reporter_id: string
          resolved_by: string | null
          status: string
        }
        ComputedFields: never
        Insert: {
          created_at?: string | null
          detail?: string | null
          id?: string | null
          post_id?: string | null
          reason?: string | null
          reporter_id?: string | null
          resolved_by?: string | null
          status?: string | null
        }
        Update: {
          created_at?: string | null
          detail?: string | null
          id?: string | null
          post_id?: string | null
          reason?: string | null
          reporter_id?: string | null
          resolved_by?: string | null
          status?: string | null
        }
        Relationships: []
      }
      profiles: {
        Row: {
          age: string | null
          avatar_url: string | null
          bio: string | null
          created_at: string | null
          full_name: string | null
          gender: string | null
          id: string
          location: string | null
          privacy_level: Database['public']['Enums']['privacy_level'] | null
          suspended_at: string | null
          updated_at: string | null
          username: string
        }
        ComputedFields: never
        Insert: {
          age?: string | null
          avatar_url?: string | null
          bio?: string | null
          created_at?: string | null
          full_name?: string | null
          gender?: string | null
          id?: string | null
          location?: string | null
          privacy_level?: Database['public']['Enums']['privacy_level'] | null
          suspended_at?: string | null
          updated_at?: string | null
          username?: string | null
        }
        Update: {
          age?: string | null
          avatar_url?: string | null
          bio?: string | null
          created_at?: string | null
          full_name?: string | null
          gender?: string | null
          id?: string | null
          location?: string | null
          privacy_level?: Database['public']['Enums']['privacy_level'] | null
          suspended_at?: string | null
          updated_at?: string | null
          username?: string | null
        }
        Relationships: []
      }
      report_target_types: {
        Row: {
          autor_fn: unknown
          created_at: string
          etiqueta: string
          key: string
          ocultar_fn: unknown
          referencia: string
          resolver_fn: unknown
          resumen_fn: unknown
        }
        ComputedFields: never
        Insert: {
          autor_fn?: unknown
          created_at?: string | null
          etiqueta?: string | null
          key?: string | null
          ocultar_fn?: unknown
          referencia?: string | null
          resolver_fn?: unknown
          resumen_fn?: unknown
        }
        Update: {
          autor_fn?: unknown
          created_at?: string | null
          etiqueta?: string | null
          key?: string | null
          ocultar_fn?: unknown
          referencia?: string | null
          resolver_fn?: unknown
          resumen_fn?: unknown
        }
        Relationships: []
      }
      route_likes: {
        Row: {
          created_at: string
          route_id: string
          user_id: string
        }
        ComputedFields: never
        Insert: {
          created_at?: string | null
          route_id?: string | null
          user_id?: string | null
        }
        Update: {
          created_at?: string | null
          route_id?: string | null
          user_id?: string | null
        }
        Relationships: []
      }
      route_photos: {
        Row: {
          created_at: string
          id: string
          path: string
          route_id: string
          user_id: string
        }
        ComputedFields: never
        Insert: {
          created_at?: string | null
          id?: string | null
          path?: string | null
          route_id?: string | null
          user_id?: string | null
        }
        Update: {
          created_at?: string | null
          id?: string | null
          path?: string | null
          route_id?: string | null
          user_id?: string | null
        }
        Relationships: []
      }
      route_reviews: {
        Row: {
          comment: string
          created_at: string
          id: string
          rating: number
          route_id: string
          user_id: string
        }
        ComputedFields: never
        Insert: {
          comment?: string | null
          created_at?: string | null
          id?: string | null
          rating?: number | null
          route_id?: string | null
          user_id?: string | null
        }
        Update: {
          comment?: string | null
          created_at?: string | null
          id?: string | null
          rating?: number | null
          route_id?: string | null
          user_id?: string | null
        }
        Relationships: []
      }
      route_runs: {
        Row: {
          activity_id: string
          created_at: string
          route_id: string
          user_id: string
        }
        ComputedFields: never
        Insert: {
          activity_id?: string | null
          created_at?: string | null
          route_id?: string | null
          user_id?: string | null
        }
        Update: {
          activity_id?: string | null
          created_at?: string | null
          route_id?: string | null
          user_id?: string | null
        }
        Relationships: []
      }
      routes: {
        Row: {
          author_id: string | null
          category: string | null
          created_at: string | null
          description: string
          difficulty: string
          distance_km: number
          elevation_loss_m: number
          elevation_m: number
          elevation_profile: NonNullable<Json>
          geom: unknown
          id: string
          is_official: boolean
          is_public: boolean
          license: string | null
          likes_count: number
          name: string
          origin: string
          path: NonNullable<Json>
          rating_avg: number | null
          rating_count: number
          slug: string | null
          source: string | null
          source_activity_id: string | null
          start_point: unknown
          surface: string | null
          tags: string[]
        }
        ComputedFields: never
        Insert: {
          author_id?: string | null
          category?: string | null
          created_at?: string | null
          description?: string | null
          difficulty?: string | null
          distance_km?: number | null
          elevation_loss_m?: number | null
          elevation_m?: number | null
          elevation_profile?: Json | null
          geom?: unknown
          id?: string | null
          is_official?: boolean | null
          is_public?: boolean | null
          license?: string | null
          likes_count?: number | null
          name?: string | null
          origin?: string | null
          path?: Json | null
          rating_avg?: number | null
          rating_count?: number | null
          slug?: string | null
          source?: string | null
          source_activity_id?: string | null
          start_point?: unknown
          surface?: string | null
          tags?: string[] | null
        }
        Update: {
          author_id?: string | null
          category?: string | null
          created_at?: string | null
          description?: string | null
          difficulty?: string | null
          distance_km?: number | null
          elevation_loss_m?: number | null
          elevation_m?: number | null
          elevation_profile?: Json | null
          geom?: unknown
          id?: string | null
          is_official?: boolean | null
          is_public?: boolean | null
          license?: string | null
          likes_count?: number | null
          name?: string | null
          origin?: string | null
          path?: Json | null
          rating_avg?: number | null
          rating_count?: number | null
          slug?: string | null
          source?: string | null
          source_activity_id?: string | null
          start_point?: unknown
          surface?: string | null
          tags?: string[] | null
        }
        Relationships: []
      }
      saved_routes: {
        Row: {
          created_at: string | null
          route_id: string
          user_id: string
        }
        ComputedFields: never
        Insert: {
          created_at?: string | null
          route_id?: string | null
          user_id?: string | null
        }
        Update: {
          created_at?: string | null
          route_id?: string | null
          user_id?: string | null
        }
        Relationships: []
      }
      tap_funky: {
        Row: {
          args: string | null
          is_definer: boolean | null
          is_strict: boolean | null
          is_visible: boolean | null
          kind: unknown
          langoid: unknown
          name: unknown
          oid: unknown
          owner: unknown
          returns: string | null
          returns_set: boolean | null
          schema: unknown
          volatility: string | null
        }
        ComputedFields: never
        Relationships: []
      }
      track_batches: {
        Row: {
          activity_id: string
          client_batch_id: string
          created_at: string
          id: string
          point_count: number
        }
        ComputedFields: never
        Insert: {
          activity_id?: string | null
          client_batch_id?: string | null
          created_at?: string | null
          id?: string | null
          point_count?: number | null
        }
        Update: {
          activity_id?: string | null
          client_batch_id?: string | null
          created_at?: string | null
          id?: string | null
          point_count?: number | null
        }
        Relationships: []
      }
      track_points: {
        Row: {
          accuracy_meters: number | null
          activity_id: string
          bearing_degrees: number | null
          created_at: string | null
          elevation_meters: number | null
          id: string
          location: unknown
          speed_mps: number | null
          timestamp: string
        }
        ComputedFields: never
        Insert: {
          accuracy_meters?: number | null
          activity_id?: string | null
          bearing_degrees?: number | null
          created_at?: string | null
          elevation_meters?: number | null
          id?: string | null
          location?: unknown
          speed_mps?: number | null
          timestamp?: string | null
        }
        Update: {
          accuracy_meters?: number | null
          activity_id?: string | null
          bearing_degrees?: number | null
          created_at?: string | null
          elevation_meters?: number | null
          id?: string | null
          location?: unknown
          speed_mps?: number | null
          timestamp?: string | null
        }
        Relationships: []
      }
      user_achievements: {
        Row: {
          code: string
          unlocked_at: string
          user_id: string
        }
        ComputedFields: never
        Insert: {
          code?: string | null
          unlocked_at?: string | null
          user_id?: string | null
        }
        Update: {
          code?: string | null
          unlocked_at?: string | null
          user_id?: string | null
        }
        Relationships: []
      }
      user_roles: {
        Row: {
          granted_at: string
          role: string
          user_id: string
        }
        ComputedFields: never
        Insert: {
          granted_at?: string | null
          role?: string | null
          user_id?: string | null
        }
        Update: {
          granted_at?: string | null
          role?: string | null
          user_id?: string | null
        }
        Relationships: []
      }
      user_settings: {
        Row: {
          hide_home: boolean
          home_radius_m: number
          precise_location: boolean
          prefs: NonNullable<Json>
          updated_at: string | null
          user_id: string
          visibility: Database['public']['Enums']['privacy_level']
          weekly_goal_runs: number
        }
        ComputedFields: never
        Insert: {
          hide_home?: boolean | null
          home_radius_m?: number | null
          precise_location?: boolean | null
          prefs?: Json | null
          updated_at?: string | null
          user_id?: string | null
          visibility?: Database['public']['Enums']['privacy_level'] | null
          weekly_goal_runs?: number | null
        }
        Update: {
          hide_home?: boolean | null
          home_radius_m?: number | null
          precise_location?: boolean | null
          prefs?: Json | null
          updated_at?: string | null
          user_id?: string | null
          visibility?: Database['public']['Enums']['privacy_level'] | null
          weekly_goal_runs?: number | null
        }
        Relationships: []
      }
      user_streaks: {
        Row: {
          best_daily: number
          best_weekly: number
          current_daily: number
          current_weekly: number
          last_active_date: string | null
          runs_this_week: number
          updated_at: string
          user_id: string
          weekly_goal_runs: number
        }
        ComputedFields: never
        Insert: {
          best_daily?: number | null
          best_weekly?: number | null
          current_daily?: number | null
          current_weekly?: number | null
          last_active_date?: string | null
          runs_this_week?: number | null
          updated_at?: string | null
          user_id?: string | null
          weekly_goal_runs?: number | null
        }
        Update: {
          best_daily?: number | null
          best_weekly?: number | null
          current_daily?: number | null
          current_weekly?: number | null
          last_active_date?: string | null
          runs_this_week?: number | null
          updated_at?: string | null
          user_id?: string | null
          weekly_goal_runs?: number | null
        }
        Relationships: []
      }
    }
    Functions: {
      _cleanup: { Args: Record<PropertyKey, never>; Returns: boolean }
      _contract_on: { Args: { '': string }; Returns: unknown }
      _currtest: { Args: Record<PropertyKey, never>; Returns: number }
      _db_privs: { Args: Record<PropertyKey, never>; Returns: unknown[] }
      _extensions: { Args: Record<PropertyKey, never>; Returns: unknown[] }
      _get: { Args: { '': string }; Returns: number }
      _get_latest: { Args: { '': string }; Returns: number[] }
      _get_note: { Args: { '': string }; Returns: string }
      _is_verbose: { Args: Record<PropertyKey, never>; Returns: boolean }
      _postgis_deprecate: {
        Args: { newname: string; oldname: string; version: string }
        Returns: undefined
      }
      _postgis_index_extent: { Args: { col: string; tbl: unknown }; Returns: unknown }
      _postgis_pgsql_version: { Args: Record<PropertyKey, never>; Returns: string }
      _postgis_scripts_pgsql_version: { Args: Record<PropertyKey, never>; Returns: string }
      _postgis_selectivity: {
        Args: { att_name: string; geom: unknown; mode?: string; tbl: unknown }
        Returns: number
      }
      _postgis_stats: { Args: { ''?: string; att_name: string; tbl: unknown }; Returns: string }
      _prokind: { Args: { p_oid: unknown }; Returns: unknown }
      _query: { Args: { '': string }; Returns: string }
      _refine_vol: { Args: { '': string }; Returns: string }
      _st_3dintersects: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      _st_contains: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      _st_containsproperly: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      _st_coveredby:
        | { Args: { geog1: unknown; geog2: unknown }; Returns: boolean }
        | { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      _st_covers:
        | { Args: { geog1: unknown; geog2: unknown }; Returns: boolean }
        | { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      _st_crosses: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      _st_dwithin: {
        Args: { geog1: unknown; geog2: unknown; tolerance: number; use_spheroid?: boolean }
        Returns: boolean
      }
      _st_equals: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      _st_intersects: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      _st_linecrossingdirection: { Args: { line1: unknown; line2: unknown }; Returns: number }
      _st_longestline: { Args: { geom1: unknown; geom2: unknown }; Returns: unknown }
      _st_maxdistance: { Args: { geom1: unknown; geom2: unknown }; Returns: number }
      _st_orderingequals: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      _st_overlaps: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      _st_sortablehash: { Args: { geom: unknown }; Returns: number }
      _st_touches: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      _st_voronoi: {
        Args: { clip?: unknown; g1: unknown; return_polygons?: boolean; tolerance?: number }
        Returns: unknown
      }
      _st_within: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      _table_privs: { Args: Record<PropertyKey, never>; Returns: unknown[] }
      _temptypes: { Args: { '': string }; Returns: string }
      _todo: { Args: Record<PropertyKey, never>; Returns: string }
      addauth: { Args: { '': string }; Returns: boolean }
      addgeometrycolumn:
        | {
            Args: {
              catalog_name: string
              column_name: string
              new_dim: number
              new_srid_in: number
              new_type: string
              schema_name: string
              table_name: string
              use_typmod?: boolean
            }
            Returns: string
          }
        | {
            Args: {
              column_name: string
              new_dim: number
              new_srid: number
              new_type: string
              schema_name: string
              table_name: string
              use_typmod?: boolean
            }
            Returns: string
          }
        | {
            Args: {
              column_name: string
              new_dim: number
              new_srid: number
              new_type: string
              table_name: string
              use_typmod?: boolean
            }
            Returns: string
          }
      admin_cola_reportes: {
        Args: { estado?: string; tipo?: string }
        Returns: {
          acciones: Json
          created_at: string
          detail: string
          id: string
          reason: string
          reportante: string
          resumen: string
          status: string
          target_id: string
          target_type: string
          total_reportes: number
          updated_at: string
        }[]
      }
      admin_estadisticas: { Args: { desde: string; hasta: string }; Returns: Json }
      admin_metricas: { Args: Record<PropertyKey, never>; Returns: Json }
      admin_suspender: { Args: { suspender: boolean; uid: string }; Returns: undefined }
      admin_usuarios: {
        Args: Record<PropertyKey, never>
        Returns: {
          avatar_url: string
          carreras: number
          created_at: string
          full_name: string
          id: string
          km: number
          role: string
          suspended_at: string
          username: string
        }[]
      }
      append_track_batch: {
        Args: { p_activity_id: string; p_client_batch_id: string; p_points: Json }
        Returns: number
      }
      avisar_rachas_en_riesgo: { Args: Record<PropertyKey, never>; Returns: number }
      can_view_user: { Args: { owner: string }; Returns: boolean }
      col_is_null:
        | {
            Args: {
              column_name: unknown
              description?: string
              schema_name: unknown
              table_name: unknown
            }
            Returns: string
          }
        | {
            Args: { column_name: unknown; description?: string; table_name: unknown }
            Returns: string
          }
      col_not_null:
        | {
            Args: {
              column_name: unknown
              description?: string
              schema_name: unknown
              table_name: unknown
            }
            Returns: string
          }
        | {
            Args: { column_name: unknown; description?: string; table_name: unknown }
            Returns: string
          }
      comentarios_publicacion: {
        Args: { p_post: string }
        Returns: {
          autor: string
          body: string
          created_at: string
          id: string
          user_id: string
        }[]
      }
      consentimiento_vigente: { Args: { tipo: string; uid: string }; Returns: boolean }
      corredores_ruta: {
        Args: { ruta: string }
        Returns: {
          nombre: string
          total: number
          ultima: string
          user_id: string
          usuario: string
          veces: number
        }[]
      }
      diag:
        | {
            Args: { msg: unknown }
            Returns: {
              error: true
            } & 'Could not choose the best candidate function between: public.diag(msg => text), public.diag(msg => anyelement). Try renaming the parameters or the function itself in the database so function overloading can be resolved'
          }
        | {
            Args: { msg: string }
            Returns: {
              error: true
            } & 'Could not choose the best candidate function between: public.diag(msg => text), public.diag(msg => anyelement). Try renaming the parameters or the function itself in the database so function overloading can be resolved'
          }
      diag_test_name: { Args: { '': string }; Returns: string }
      disablelongtransactions: { Args: Record<PropertyKey, never>; Returns: string }
      do_tap:
        | { Args: Record<PropertyKey, never>; Returns: string[] }
        | { Args: { '': string }; Returns: string[] }
      dropgeometrycolumn:
        | {
            Args: {
              catalog_name: string
              column_name: string
              schema_name: string
              table_name: string
            }
            Returns: string
          }
        | {
            Args: { column_name: string; schema_name: string; table_name: string }
            Returns: string
          }
        | { Args: { column_name: string; table_name: string }; Returns: string }
      dropgeometrytable:
        | {
            Args: { catalog_name: string; schema_name: string; table_name: string }
            Returns: string
          }
        | { Args: { schema_name: string; table_name: string }; Returns: string }
        | { Args: { table_name: string }; Returns: string }
      eliminar_mi_cuenta: { Args: Record<PropertyKey, never>; Returns: undefined }
      enablelongtransactions: { Args: Record<PropertyKey, never>; Returns: string }
      equals: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      es_moderador: { Args: { uid?: string }; Returns: boolean }
      event_participants: { Args: { eid: string }; Returns: number }
      evento_en_vivo: {
        Args: { evento: string }
        Returns: {
          distance_m: number
          elapsed_s: number
          ended: boolean
          hidden: boolean
          last_lat: number
          last_lng: number
          nombre: string
          points: Json
          session_id: string
          user_id: string
        }[]
      }
      evento_estado: {
        Args: { evento: string }
        Returns: {
          cupo: number
          en_espera: number
          inscritos: number
          mi_estado: string
          mi_posicion: number
        }[]
      }
      eventos_recordatorios: { Args: Record<PropertyKey, never>; Returns: number }
      fail:
        | { Args: Record<PropertyKey, never>; Returns: string }
        | { Args: { '': string }; Returns: string }
      feed_comunidad: {
        Args: { p_antes?: string; p_limite?: number }
        Returns: {
          autor: string
          autor_km: number
          avatar_url: string
          client_id: string
          comments_count: number
          distance_meters: number
          duration_seconds: number
          elevation_gain: number
          id: string
          images: Json
          likes_count: number
          me_gusta: boolean
          propia: boolean
          public_points: Json
          reposteada: boolean
          reposts_count: number
          sigo: boolean
          start_time: string
          title: string
          ultimo_comentario: Json
          user_id: string
        }[]
      }
      findfuncs: { Args: { '': string }; Returns: string[] }
      finish: { Args: { exception_on_failure?: boolean }; Returns: string[] }
      geometry: { Args: { '': string }; Returns: unknown }
      geometry_above: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      geometry_below: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      geometry_cmp: { Args: { geom1: unknown; geom2: unknown }; Returns: number }
      geometry_contained_3d: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      geometry_contains: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      geometry_contains_3d: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      geometry_distance_box: { Args: { geom1: unknown; geom2: unknown }; Returns: number }
      geometry_distance_centroid: { Args: { geom1: unknown; geom2: unknown }; Returns: number }
      geometry_eq: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      geometry_ge: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      geometry_gt: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      geometry_le: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      geometry_left: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      geometry_lt: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      geometry_overabove: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      geometry_overbelow: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      geometry_overlaps: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      geometry_overlaps_3d: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      geometry_overleft: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      geometry_overright: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      geometry_right: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      geometry_same: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      geometry_same_3d: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      geometry_within: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      geomfromewkt: { Args: { '': string }; Returns: unknown }
      gettransactionid: { Args: Record<PropertyKey, never>; Returns: unknown }
      has_unique: { Args: { '': string }; Returns: string }
      in_todo: { Args: Record<PropertyKey, never>; Returns: boolean }
      is_admin: { Args: { uid?: string }; Returns: boolean }
      is_empty: { Args: { '': string }; Returns: string }
      isnt_empty: { Args: { '': string }; Returns: string }
      live_cerrar_inactivas: { Args: Record<PropertyKey, never>; Returns: number }
      live_enviar: {
        Args: { distancia_m: number; lat: number; lng: number; segundos: number; sesion: string }
        Returns: boolean
      }
      live_iniciar: { Args: { audiencia?: string }; Returns: string }
      live_iniciar_evento: { Args: { evento: string }; Returns: string }
      live_terminar: { Args: { sesion: string }; Returns: undefined }
      lives_ok: { Args: { '': string }; Returns: string }
      longtransactionsenabled: { Args: Record<PropertyKey, never>; Returns: boolean }
      mapa_publicar: { Args: { corriendo: boolean; lat: number; lng: number }; Returns: boolean }
      mi_racha: {
        Args: Record<PropertyKey, never>
        Returns: Database['public']['Views']['user_streaks']['Row']
        SetofOptions: {
          from: '*'
          to: 'user_streaks'
          isOneToOne: true
          isSetofReturn: false
        }
      }
      moderacion_autor_comentario: { Args: { objetivo: string }; Returns: string }
      moderacion_autor_evento: { Args: { objetivo: string }; Returns: string }
      moderacion_autor_incidente: { Args: { objetivo: string }; Returns: string }
      moderacion_autor_post: { Args: { objetivo: string }; Returns: string }
      moderacion_autor_usuario: { Args: { objetivo: string }; Returns: string }
      moderacion_ocultar_comentario: { Args: { objetivo: string }; Returns: undefined }
      moderacion_ocultar_evento: { Args: { objetivo: string }; Returns: undefined }
      moderacion_ocultar_incidente: { Args: { objetivo: string }; Returns: undefined }
      moderacion_ocultar_post: { Args: { objetivo: string }; Returns: undefined }
      moderacion_ocultar_usuario: { Args: { objetivo: string }; Returns: undefined }
      moderacion_resolver_incidente: {
        Args: { objetivo: string; resultado: string }
        Returns: undefined
      }
      moderacion_resumen: { Args: { objetivo: string; tipo: string }; Returns: string }
      moderacion_resumen_comentario: { Args: { objetivo: string }; Returns: string }
      moderacion_resumen_evento: { Args: { objetivo: string }; Returns: string }
      moderacion_resumen_incidente: { Args: { objetivo: string }; Returns: string }
      moderacion_resumen_post: { Args: { objetivo: string }; Returns: string }
      moderacion_resumen_usuario: { Args: { objetivo: string }; Returns: string }
      moderar_reporte: {
        Args: { accion: string; nota?: string; reporte: string }
        Returns: Database['public']['Views']['content_reports']['Row']
        SetofOptions: {
          from: '*'
          to: 'content_reports'
          isOneToOne: true
          isSetofReturn: false
        }
      }
      nivel_seguridad_ruta: {
        Args: { p_dia_semana: number; p_hora?: number; p_ruta_id: string }
        Returns: {
          cod_parroquia: string
          indice: number
          nivel: string
          parroquia: string
          reportes_usuarios: number
          tramo: number
        }[]
      }
      no_plan: { Args: Record<PropertyKey, never>; Returns: boolean[] }
      num_failed: { Args: Record<PropertyKey, never>; Returns: number }
      os_name: { Args: Record<PropertyKey, never>; Returns: string }
      participantes_eventos: {
        Args: Record<PropertyKey, never>
        Returns: {
          event_id: string
          otros: number
        }[]
      }
      pass:
        | { Args: Record<PropertyKey, never>; Returns: string }
        | { Args: { '': string }; Returns: string }
      pg_version: { Args: Record<PropertyKey, never>; Returns: string }
      pg_version_num: { Args: Record<PropertyKey, never>; Returns: number }
      pgtap_version: { Args: Record<PropertyKey, never>; Returns: number }
      populate_geometry_columns:
        | { Args: { tbl_oid: unknown; use_typmod?: boolean }; Returns: number }
        | { Args: { use_typmod?: boolean }; Returns: string }
      postgis_constraint_dims: {
        Args: { geomcolumn: string; geomschema: string; geomtable: string }
        Returns: number
      }
      postgis_constraint_srid: {
        Args: { geomcolumn: string; geomschema: string; geomtable: string }
        Returns: number
      }
      postgis_constraint_type: {
        Args: { geomcolumn: string; geomschema: string; geomtable: string }
        Returns: string
      }
      postgis_extensions_upgrade: { Args: Record<PropertyKey, never>; Returns: string }
      postgis_full_version: { Args: Record<PropertyKey, never>; Returns: string }
      postgis_geos_version: { Args: Record<PropertyKey, never>; Returns: string }
      postgis_lib_build_date: { Args: Record<PropertyKey, never>; Returns: string }
      postgis_lib_revision: { Args: Record<PropertyKey, never>; Returns: string }
      postgis_lib_version: { Args: Record<PropertyKey, never>; Returns: string }
      postgis_libjson_version: { Args: Record<PropertyKey, never>; Returns: string }
      postgis_liblwgeom_version: { Args: Record<PropertyKey, never>; Returns: string }
      postgis_libprotobuf_version: { Args: Record<PropertyKey, never>; Returns: string }
      postgis_libxml_version: { Args: Record<PropertyKey, never>; Returns: string }
      postgis_proj_version: { Args: Record<PropertyKey, never>; Returns: string }
      postgis_scripts_build_date: { Args: Record<PropertyKey, never>; Returns: string }
      postgis_scripts_installed: { Args: Record<PropertyKey, never>; Returns: string }
      postgis_scripts_released: { Args: Record<PropertyKey, never>; Returns: string }
      postgis_svn_version: { Args: Record<PropertyKey, never>; Returns: string }
      postgis_type_name: {
        Args: { coord_dimension: number; geomname: string; use_new_name?: boolean }
        Returns: string
      }
      postgis_version: { Args: Record<PropertyKey, never>; Returns: string }
      postgis_wagyu_version: { Args: Record<PropertyKey, never>; Returns: string }
      puede_subir_vista_previa: { Args: { nombre: string }; Returns: boolean }
      puede_ver_en_vivo: { Args: { audiencia: string; dueno: string }; Returns: boolean }
      recalcular_racha: {
        Args: { notificar?: boolean; uid: string }
        Returns: Database['public']['Views']['user_streaks']['Row']
        SetofOptions: {
          from: '*'
          to: 'user_streaks'
          isOneToOne: true
          isSetofReturn: false
        }
      }
      reemplazar_incidentes_ecu911: {
        Args: { p_agregados: Json; p_archivo: string; p_filas: number; p_filas_loja: number }
        Returns: number
      }
      registrar_error_ecu911: { Args: { p_archivo: string; p_mensaje: string }; Returns: undefined }
      reporte_personal: { Args: { periodo: string; referencia?: string }; Returns: Json }
      rol_club: { Args: { club: string; usuario?: string }; Returns: string }
      runtests:
        | { Args: Record<PropertyKey, never>; Returns: string[] }
        | { Args: { '': string }; Returns: string[] }
      rutas_cercanas: {
        Args: { lat: number; lng: number; radio_km?: number }
        Returns: {
          distancia_km: number
          id: string
        }[]
      }
      seguridad_zona_ruta: { Args: { p_ruta_id: string }; Returns: Json }
      skip:
        | { Args: { '': string }; Returns: string }
        | { Args: { how_many: number; why: string }; Returns: string }
      st_3dclosestpoint: { Args: { geom1: unknown; geom2: unknown }; Returns: unknown }
      st_3ddistance: { Args: { geom1: unknown; geom2: unknown }; Returns: number }
      st_3dintersects: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      st_3dlongestline: { Args: { geom1: unknown; geom2: unknown }; Returns: unknown }
      st_3dmakebox: { Args: { geom1: unknown; geom2: unknown }; Returns: unknown }
      st_3dmaxdistance: { Args: { geom1: unknown; geom2: unknown }; Returns: number }
      st_3dshortestline: { Args: { geom1: unknown; geom2: unknown }; Returns: unknown }
      st_addpoint: { Args: { geom1: unknown; geom2: unknown }; Returns: unknown }
      st_angle:
        | { Args: { line1: unknown; line2: unknown }; Returns: number }
        | { Args: { pt1: unknown; pt2: unknown; pt3: unknown; pt4?: unknown }; Returns: number }
      st_area:
        | { Args: { geog: unknown; use_spheroid?: boolean }; Returns: number }
        | { Args: { '': string }; Returns: number }
      st_asencodedpolyline: { Args: { geom: unknown; nprecision?: number }; Returns: string }
      st_asewkt: { Args: { '': string }; Returns: string }
      st_asgeojson:
        | { Args: { geog: unknown; maxdecimaldigits?: number; options?: number }; Returns: string }
        | { Args: { geom: unknown; maxdecimaldigits?: number; options?: number }; Returns: string }
        | {
            Args: {
              geom_column?: string
              maxdecimaldigits?: number
              pretty_bool?: boolean
              r: Record<string, unknown>
            }
            Returns: string
          }
        | { Args: { '': string }; Returns: string }
      st_asgml:
        | {
            Args: {
              geog: unknown
              id?: string
              maxdecimaldigits?: number
              nprefix?: string
              options?: number
            }
            Returns: string
          }
        | { Args: { geom: unknown; maxdecimaldigits?: number; options?: number }; Returns: string }
        | { Args: { '': string }; Returns: string }
        | {
            Args: {
              geog: unknown
              id?: string
              maxdecimaldigits?: number
              nprefix?: string
              options?: number
              version: number
            }
            Returns: string
          }
        | {
            Args: {
              geom: unknown
              id?: string
              maxdecimaldigits?: number
              nprefix?: string
              options?: number
              version: number
            }
            Returns: string
          }
      st_askml:
        | { Args: { geog: unknown; maxdecimaldigits?: number; nprefix?: string }; Returns: string }
        | { Args: { geom: unknown; maxdecimaldigits?: number; nprefix?: string }; Returns: string }
        | { Args: { '': string }; Returns: string }
      st_aslatlontext: { Args: { geom: unknown; tmpl?: string }; Returns: string }
      st_asmarc21: { Args: { format?: string; geom: unknown }; Returns: string }
      st_asmvtgeom: {
        Args: {
          bounds: unknown
          buffer?: number
          clip_geom?: boolean
          extent?: number
          geom: unknown
        }
        Returns: unknown
      }
      st_assvg:
        | { Args: { geog: unknown; maxdecimaldigits?: number; rel?: number }; Returns: string }
        | { Args: { geom: unknown; maxdecimaldigits?: number; rel?: number }; Returns: string }
        | { Args: { '': string }; Returns: string }
      st_astext: { Args: { '': string }; Returns: string }
      st_astwkb:
        | {
            Args: {
              geom: unknown
              prec?: number
              prec_m?: number
              prec_z?: number
              with_boxes?: boolean
              with_sizes?: boolean
            }
            Returns: string
          }
        | {
            Args: {
              geom: unknown[]
              ids: number[]
              prec?: number
              prec_m?: number
              prec_z?: number
              with_boxes?: boolean
              with_sizes?: boolean
            }
            Returns: string
          }
      st_asx3d: {
        Args: { geom: unknown; maxdecimaldigits?: number; options?: number }
        Returns: string
      }
      st_azimuth:
        | { Args: { geog1: unknown; geog2: unknown }; Returns: number }
        | { Args: { geom1: unknown; geom2: unknown }; Returns: number }
      st_boundingdiagonal: { Args: { fits?: boolean; geom: unknown }; Returns: unknown }
      st_buffer:
        | { Args: { geom: unknown; options?: string; radius: number }; Returns: unknown }
        | { Args: { geom: unknown; quadsegs: number; radius: number }; Returns: unknown }
      st_centroid: { Args: { '': string }; Returns: unknown }
      st_clipbybox2d: { Args: { box: unknown; geom: unknown }; Returns: unknown }
      st_closestpoint: { Args: { geom1: unknown; geom2: unknown }; Returns: unknown }
      st_collect: { Args: { geom1: unknown; geom2: unknown }; Returns: unknown }
      st_concavehull: {
        Args: { param_allow_holes?: boolean; param_geom: unknown; param_pctconvex: number }
        Returns: unknown
      }
      st_contains: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      st_containsproperly: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      st_coorddim: { Args: { geometry: unknown }; Returns: number }
      st_coveredby:
        | { Args: { geog1: unknown; geog2: unknown }; Returns: boolean }
        | { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      st_covers:
        | { Args: { geog1: unknown; geog2: unknown }; Returns: boolean }
        | { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      st_crosses: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      st_curvetoline: {
        Args: { flags?: number; geom: unknown; tol?: number; toltype?: number }
        Returns: unknown
      }
      st_delaunaytriangles: {
        Args: { flags?: number; g1: unknown; tolerance?: number }
        Returns: unknown
      }
      st_difference: {
        Args: { geom1: unknown; geom2: unknown; gridsize?: number }
        Returns: unknown
      }
      st_disjoint: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      st_distance:
        | { Args: { geog1: unknown; geog2: unknown; use_spheroid?: boolean }; Returns: number }
        | { Args: { geom1: unknown; geom2: unknown }; Returns: number }
      st_distancesphere:
        | { Args: { geom1: unknown; geom2: unknown }; Returns: number }
        | { Args: { geom1: unknown; geom2: unknown; radius: number }; Returns: number }
      st_distancespheroid: { Args: { geom1: unknown; geom2: unknown }; Returns: number }
      st_dwithin: {
        Args: { geog1: unknown; geog2: unknown; tolerance: number; use_spheroid?: boolean }
        Returns: boolean
      }
      st_equals: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      st_expand:
        | { Args: { box: unknown; dx: number; dy: number }; Returns: unknown }
        | { Args: { box: unknown; dx: number; dy: number; dz?: number }; Returns: unknown }
        | {
            Args: { dm?: number; dx: number; dy: number; dz?: number; geom: unknown }
            Returns: unknown
          }
      st_force3d: { Args: { geom: unknown; zvalue?: number }; Returns: unknown }
      st_force3dm: { Args: { geom: unknown; mvalue?: number }; Returns: unknown }
      st_force3dz: { Args: { geom: unknown; zvalue?: number }; Returns: unknown }
      st_force4d: { Args: { geom: unknown; mvalue?: number; zvalue?: number }; Returns: unknown }
      st_generatepoints:
        | { Args: { area: unknown; npoints: number }; Returns: unknown }
        | { Args: { area: unknown; npoints: number; seed: number }; Returns: unknown }
      st_geogfromtext: { Args: { '': string }; Returns: unknown }
      st_geographyfromtext: { Args: { '': string }; Returns: unknown }
      st_geohash:
        | { Args: { geog: unknown; maxchars?: number }; Returns: string }
        | { Args: { geom: unknown; maxchars?: number }; Returns: string }
      st_geomcollfromtext: { Args: { '': string }; Returns: unknown }
      st_geometricmedian: {
        Args: { fail_if_not_converged?: boolean; g: unknown; max_iter?: number; tolerance?: number }
        Returns: unknown
      }
      st_geometryfromtext: { Args: { '': string }; Returns: unknown }
      st_geomfromewkt: { Args: { '': string }; Returns: unknown }
      st_geomfromgeojson:
        | { Args: { '': Json }; Returns: unknown }
        | { Args: { '': Json }; Returns: unknown }
        | { Args: { '': string }; Returns: unknown }
      st_geomfromgml: { Args: { '': string }; Returns: unknown }
      st_geomfromkml: { Args: { '': string }; Returns: unknown }
      st_geomfrommarc21: { Args: { marc21xml: string }; Returns: unknown }
      st_geomfromtext: { Args: { '': string }; Returns: unknown }
      st_gmltosql: { Args: { '': string }; Returns: unknown }
      st_hasarc: { Args: { geometry: unknown }; Returns: boolean }
      st_hausdorffdistance: { Args: { geom1: unknown; geom2: unknown }; Returns: number }
      st_hexagon: {
        Args: { cell_i: number; cell_j: number; origin?: unknown; size: number }
        Returns: unknown
      }
      st_hexagongrid: {
        Args: { bounds: unknown; size: number }
        Returns: Record<string, unknown>[]
      }
      st_interpolatepoint: { Args: { line: unknown; point: unknown }; Returns: number }
      st_intersection: {
        Args: { geom1: unknown; geom2: unknown; gridsize?: number }
        Returns: unknown
      }
      st_intersects:
        | { Args: { geog1: unknown; geog2: unknown }; Returns: boolean }
        | { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      st_isvaliddetail: {
        Args: { flags?: number; geom: unknown }
        Returns: Database['public']['CompositeTypes']['valid_detail']
        SetofOptions: {
          from: '*'
          to: 'valid_detail'
          isOneToOne: true
          isSetofReturn: false
        }
      }
      st_length:
        | { Args: { geog: unknown; use_spheroid?: boolean }; Returns: number }
        | { Args: { '': string }; Returns: number }
      st_letters: { Args: { font?: Json; letters: string }; Returns: unknown }
      st_linecrossingdirection: { Args: { line1: unknown; line2: unknown }; Returns: number }
      st_linefromencodedpolyline: { Args: { nprecision?: number; txtin: string }; Returns: unknown }
      st_linefromtext: { Args: { '': string }; Returns: unknown }
      st_linelocatepoint: { Args: { geom1: unknown; geom2: unknown }; Returns: number }
      st_linetocurve: { Args: { geometry: unknown }; Returns: unknown }
      st_locatealong: {
        Args: { geometry: unknown; leftrightoffset?: number; measure: number }
        Returns: unknown
      }
      st_locatebetween: {
        Args: {
          frommeasure: number
          geometry: unknown
          leftrightoffset?: number
          tomeasure: number
        }
        Returns: unknown
      }
      st_locatebetweenelevations: {
        Args: { fromelevation: number; geometry: unknown; toelevation: number }
        Returns: unknown
      }
      st_longestline: { Args: { geom1: unknown; geom2: unknown }; Returns: unknown }
      st_makebox2d: { Args: { geom1: unknown; geom2: unknown }; Returns: unknown }
      st_makeline: { Args: { geom1: unknown; geom2: unknown }; Returns: unknown }
      st_makevalid: { Args: { geom: unknown; params: string }; Returns: unknown }
      st_maxdistance: { Args: { geom1: unknown; geom2: unknown }; Returns: number }
      st_minimumboundingcircle: {
        Args: { inputgeom: unknown; segs_per_quarter?: number }
        Returns: unknown
      }
      st_mlinefromtext: { Args: { '': string }; Returns: unknown }
      st_mpointfromtext: { Args: { '': string }; Returns: unknown }
      st_mpolyfromtext: { Args: { '': string }; Returns: unknown }
      st_multilinestringfromtext: { Args: { '': string }; Returns: unknown }
      st_multipointfromtext: { Args: { '': string }; Returns: unknown }
      st_multipolygonfromtext: { Args: { '': string }; Returns: unknown }
      st_node: { Args: { g: unknown }; Returns: unknown }
      st_normalize: { Args: { geom: unknown }; Returns: unknown }
      st_offsetcurve: {
        Args: { distance: number; line: unknown; params?: string }
        Returns: unknown
      }
      st_orderingequals: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      st_overlaps: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      st_perimeter: { Args: { geog: unknown; use_spheroid?: boolean }; Returns: number }
      st_pointfromtext: { Args: { '': string }; Returns: unknown }
      st_pointm: {
        Args: { mcoordinate: number; srid?: number; xcoordinate: number; ycoordinate: number }
        Returns: unknown
      }
      st_pointz: {
        Args: { srid?: number; xcoordinate: number; ycoordinate: number; zcoordinate: number }
        Returns: unknown
      }
      st_pointzm: {
        Args: {
          mcoordinate: number
          srid?: number
          xcoordinate: number
          ycoordinate: number
          zcoordinate: number
        }
        Returns: unknown
      }
      st_polyfromtext: { Args: { '': string }; Returns: unknown }
      st_polygonfromtext: { Args: { '': string }; Returns: unknown }
      st_project: { Args: { azimuth: number; distance: number; geog: unknown }; Returns: unknown }
      st_quantizecoordinates: {
        Args: { g: unknown; prec_m?: number; prec_x: number; prec_y?: number; prec_z?: number }
        Returns: unknown
      }
      st_reduceprecision: { Args: { geom: unknown; gridsize: number }; Returns: unknown }
      st_relate: { Args: { geom1: unknown; geom2: unknown }; Returns: string }
      st_removerepeatedpoints: { Args: { geom: unknown; tolerance?: number }; Returns: unknown }
      st_segmentize: { Args: { geog: unknown; max_segment_length: number }; Returns: unknown }
      st_setsrid:
        | { Args: { geog: unknown; srid: number }; Returns: unknown }
        | { Args: { geom: unknown; srid: number }; Returns: unknown }
      st_sharedpaths: { Args: { geom1: unknown; geom2: unknown }; Returns: unknown }
      st_shortestline: { Args: { geom1: unknown; geom2: unknown }; Returns: unknown }
      st_simplifypolygonhull: {
        Args: { geom: unknown; is_outer?: boolean; vertex_fraction: number }
        Returns: unknown
      }
      st_split: { Args: { geom1: unknown; geom2: unknown }; Returns: unknown }
      st_square: {
        Args: { cell_i: number; cell_j: number; origin?: unknown; size: number }
        Returns: unknown
      }
      st_squaregrid: { Args: { bounds: unknown; size: number }; Returns: Record<string, unknown>[] }
      st_srid:
        { Args: { geog: unknown }; Returns: number } | { Args: { geom: unknown }; Returns: number }
      st_subdivide: {
        Args: { geom: unknown; gridsize?: number; maxvertices?: number }
        Returns: unknown[]
      }
      st_swapordinates: { Args: { geom: unknown; ords: unknown }; Returns: unknown }
      st_symdifference: {
        Args: { geom1: unknown; geom2: unknown; gridsize?: number }
        Returns: unknown
      }
      st_symmetricdifference: { Args: { geom1: unknown; geom2: unknown }; Returns: unknown }
      st_tileenvelope: {
        Args: { bounds?: unknown; margin?: number; x: number; y: number; zoom: number }
        Returns: unknown
      }
      st_touches: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      st_transform:
        | { Args: { from_proj: string; geom: unknown; to_proj: string }; Returns: unknown }
        | { Args: { from_proj: string; geom: unknown; to_srid: number }; Returns: unknown }
        | { Args: { geom: unknown; to_proj: string }; Returns: unknown }
      st_triangulatepolygon: { Args: { g1: unknown }; Returns: unknown }
      st_union:
        | { Args: { geom1: unknown; geom2: unknown }; Returns: unknown }
        | { Args: { geom1: unknown; geom2: unknown; gridsize: number }; Returns: unknown }
      st_voronoilines: {
        Args: { extend_to?: unknown; g1: unknown; tolerance?: number }
        Returns: unknown
      }
      st_voronoipolygons: {
        Args: { extend_to?: unknown; g1: unknown; tolerance?: number }
        Returns: unknown
      }
      st_within: { Args: { geom1: unknown; geom2: unknown }; Returns: boolean }
      st_wkbtosql: { Args: { wkb: string }; Returns: unknown }
      st_wkttosql: { Args: { '': string }; Returns: unknown }
      st_wrapx: { Args: { geom: unknown; move: number; wrap: number }; Returns: unknown }
      throws_ok: { Args: { '': string }; Returns: string }
      todo:
        | { Args: { how_many: number }; Returns: boolean[] }
        | { Args: { how_many: number; why: string }; Returns: boolean[] }
        | { Args: { why: string }; Returns: boolean[] }
        | { Args: { how_many: number; why: string }; Returns: boolean[] }
      todo_end: { Args: Record<PropertyKey, never>; Returns: boolean[] }
      todo_start:
        | { Args: Record<PropertyKey, never>; Returns: boolean[] }
        | { Args: { '': string }; Returns: boolean[] }
      unlockrows: { Args: { '': string }; Returns: number }
      updategeometrysrid: {
        Args: {
          catalogn_name: string
          column_name: string
          new_srid_in: number
          schema_name: string
          table_name: string
        }
        Returns: string
      }
    }
    Enums: {
      activity_type: 'run' | 'walk' | 'hike' | 'bike' | 'other'
      privacy_level: 'public' | 'followers' | 'private'
    }
    CompositeTypes: {
      _time_trial_type: {
        a_time: number | null
      }
      geometry_dump: {
        path: number[] | null
        geom: unknown
      }
      valid_detail: {
        valid: boolean | null
        reason: string | null
        location: unknown
      }
    }
  }
}

type DatabaseWithoutInternals = Omit<Database, '__InternalSupabase'>

type DefaultSchema = DatabaseWithoutInternals[Extract<keyof Database, 'public'>]

export type Tables<
  DefaultSchemaTableNameOrOptions extends
    | keyof (DefaultSchema['Tables'] & DefaultSchema['Views'])
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends (DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions['schema']]['Tables'] &
        DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions['schema']]['Views'])
    : never) = never,
> = DefaultSchemaTableNameOrOptions extends { schema: keyof DatabaseWithoutInternals }
  ? (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions['schema']]['Tables'] &
      DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions['schema']]['Views'])[TableName] extends {
      Row: infer R
    }
    ? R
    : never
  : DefaultSchemaTableNameOrOptions extends keyof (DefaultSchema['Tables'] & DefaultSchema['Views'])
    ? (DefaultSchema['Tables'] & DefaultSchema['Views'])[DefaultSchemaTableNameOrOptions] extends {
        Row: infer R
      }
      ? R
      : never
    : never

export type TablesInsert<
  DefaultSchemaTableNameOrOptions extends
    keyof DefaultSchema['Tables'] | { schema: keyof DatabaseWithoutInternals },
  TableName extends (DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions['schema']]['Tables']
    : never) = never,
> = DefaultSchemaTableNameOrOptions extends { schema: keyof DatabaseWithoutInternals }
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions['schema']]['Tables'][TableName] extends {
      Insert: infer I
    }
    ? I
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema['Tables']
    ? DefaultSchema['Tables'][DefaultSchemaTableNameOrOptions] extends {
        Insert: infer I
      }
      ? I
      : never
    : never

export type TablesUpdate<
  DefaultSchemaTableNameOrOptions extends
    keyof DefaultSchema['Tables'] | { schema: keyof DatabaseWithoutInternals },
  TableName extends (DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions['schema']]['Tables']
    : never) = never,
> = DefaultSchemaTableNameOrOptions extends { schema: keyof DatabaseWithoutInternals }
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions['schema']]['Tables'][TableName] extends {
      Update: infer U
    }
    ? U
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema['Tables']
    ? DefaultSchema['Tables'][DefaultSchemaTableNameOrOptions] extends {
        Update: infer U
      }
      ? U
      : never
    : never

export type Enums<
  DefaultSchemaEnumNameOrOptions extends
    keyof DefaultSchema['Enums'] | { schema: keyof DatabaseWithoutInternals },
  EnumName extends (DefaultSchemaEnumNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions['schema']]['Enums']
    : never) = never,
> = DefaultSchemaEnumNameOrOptions extends { schema: keyof DatabaseWithoutInternals }
  ? DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions['schema']]['Enums'][EnumName]
  : DefaultSchemaEnumNameOrOptions extends keyof DefaultSchema['Enums']
    ? DefaultSchema['Enums'][DefaultSchemaEnumNameOrOptions]
    : never

export type CompositeTypes<
  PublicCompositeTypeNameOrOptions extends
    keyof DefaultSchema['CompositeTypes'] | { schema: keyof DatabaseWithoutInternals },
  CompositeTypeName extends (PublicCompositeTypeNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions['schema']]['CompositeTypes']
    : never) = never,
> = PublicCompositeTypeNameOrOptions extends { schema: keyof DatabaseWithoutInternals }
  ? DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions['schema']]['CompositeTypes'][CompositeTypeName]
  : PublicCompositeTypeNameOrOptions extends keyof DefaultSchema['CompositeTypes']
    ? DefaultSchema['CompositeTypes'][PublicCompositeTypeNameOrOptions]
    : never

export const Constants = {
  public: {
    Enums: {
      activity_type: ['run', 'walk', 'hike', 'bike', 'other'],
      privacy_level: ['public', 'followers', 'private'],
    },
  },
} as const
