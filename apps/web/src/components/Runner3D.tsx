import { Suspense, useRef, useEffect } from 'react';
import { Canvas } from '@react-three/fiber';
import { useGLTF, useAnimations, OrbitControls } from '@react-three/drei';
import * as THREE from 'three';

interface Runner3DProps {
  animation?: 'Idle' | 'Jog' | 'Run';
  className?: string;
  autoRotate?: boolean;
  cameraPosition?: [number, number, number];
}

function RunnerModel({ animation = 'Idle' }: { animation: string }) {
  const group = useRef<THREE.Group>(null);
  const { scene, animations } = useGLTF('/models/gacelas-runner-animated.glb');
  const { actions } = useAnimations(animations, group);

  useEffect(() => {
    if (actions[animation]) {
      actions[animation]?.reset().fadeIn(0.5).play();
      return () => {
        actions[animation]?.fadeOut(0.5);
      };
    }
  }, [actions, animation]);

  return (
    <primitive 
      ref={group} 
      object={scene.clone()} 
      scale={1.5}
      position={[0, 0, 0]}
    />
  );
}

export function Runner3D({ 
  animation = 'Idle', 
  className = '', 
  autoRotate = false,
  cameraPosition = [0, 1.2, 3]
}: Runner3DProps) {
  return (
    <div className={`w-full h-full ${className}`}>
      <Canvas
        camera={{ position: cameraPosition, fov: 50 }}
        shadows
      >
        <Suspense fallback={null}>
          <ambientLight intensity={0.5} />
          <directionalLight
            position={[5, 5, 5]}
            intensity={1}
            castShadow
          />
          <RunnerModel animation={animation} />
          <OrbitControls
            enableZoom={false}
            enablePan={false}
            autoRotate={autoRotate}
            autoRotateSpeed={2}
            maxPolarAngle={Math.PI / 2}
            minPolarAngle={Math.PI / 4}
          />
        </Suspense>
      </Canvas>
    </div>
  );
}

useGLTF.preload('/models/gacelas-runner-animated.glb');
