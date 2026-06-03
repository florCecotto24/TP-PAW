import { defineConfig } from 'vitest/config';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

export default defineConfig({
    test: {
        environment: 'node',
        include: ['src/test/js/**/*.test.js']
    },
    resolve: {
        alias: {
            '@chat-js': path.resolve(__dirname, 'src/main/webapp/js')
        }
    }
});
