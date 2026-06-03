import { describe, test, expect } from 'vitest';
import path from 'path';
import { createRequire } from 'module';

const require = createRequire(import.meta.url);
const uploadModule = require(path.resolve('src/main/webapp/js/reservation-chat-upload.js'));

describe('ReservationChatUpload', () => {
    test('computeUploadTimeoutMs enforces minimum 120 seconds', () => {
        expect(uploadModule.computeUploadTimeoutMs(1)).toBeGreaterThanOrEqual(120000);
    });

    test('computeUploadTimeoutMs scales with attachment size', () => {
        expect(uploadModule.computeUploadTimeoutMs(25)).toBeGreaterThanOrEqual(125000);
    });
});
