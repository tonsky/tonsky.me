import { defineConfig } from 'vite';

export default defineConfig({
  build: {
    outDir: '../site/presence',
    emptyOutDir: false,
    lib: {
      entry: 'src/presence.ts',
      name: 'Presence',
      fileName: 'presence',
      formats: ['es']
    },
    rollupOptions: {
      output: {
        assetFileNames: 'presence.[ext]'
      }
    }
  }
}); 