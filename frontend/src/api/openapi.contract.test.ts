import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';
import { MediaTypes } from './mediaTypes';
import type { PriceMarketPosition } from '../components/ryden/car/CarCard';
import type { ReservationStatus } from '../features/reservations/types';

const OPENAPI_PATH = resolve(import.meta.dirname, '../../../openapi.yaml');

function loadOpenApi(): string {
  return readFileSync(OPENAPI_PATH, 'utf8');
}

function enumValues(yaml: string, enumName: string): string[] {
  const multiline = new RegExp(
    `^    ${enumName}:\\s*\\r?\\n      type: string\\s*\\r?\\n      enum:\\s*\\r?\\n((?:        - .+\\r?\\n)+)`,
    'm',
  );
  const multilineMatch = yaml.match(multiline);
  if (multilineMatch) {
    return multilineMatch[1]
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter((line) => line.startsWith('- '))
      .map((line) => line.slice(2).trim());
  }

  const inline = new RegExp(
    `^    ${enumName}:\\s*\\r?\\n      type: string\\s*\\r?\\n      enum: \\[(.+?)\\]`,
    'm',
  );
  const inlineMatch = yaml.match(inline);
  if (inlineMatch) {
    return inlineMatch[1]
      .split(',')
      .map((value) => value.trim())
      .filter((value) => value.length > 0);
  }

  throw new Error(`Enum not found: ${enumName}`);
}

function schemaProperties(yaml: string, schemaName: string): string[] {
  const start = new RegExp(
    `^    ${schemaName}:\\s*\\r?\\n      type: object\\s*\\r?\\n(?:      (?!properties:).+\\r?\\n)*      properties:\\s*\\r?\\n`,
    'm',
  );
  const match = start.exec(yaml);
  if (!match) throw new Error(`Schema not found: ${schemaName}`);
  const tail = yaml.slice(match.index + match[0].length);
  const props: string[] = [];
  for (const line of tail.split(/\r?\n/)) {
    if (line.trim() === '') break;
    if (!line.startsWith('        ')) break;
    const name = line.match(/^        (\w+):/)?.[1];
    if (name) props.push(name);
  }
  return props;
}

function vendorJsonMediaTypes(yaml: string): Set<string> {
  const mime = /application\/vnd\.paw\.[^\s"']+\+json/g;
  return new Set(
    (yaml.match(mime) ?? []).filter((t) => !t.includes('<entidad>')),
  );
}

/** Estados del contrato — deben coincidir con openapi ReservationStatus. */
const FRONTEND_RESERVATION_STATUSES: ReservationStatus[] = [
  'pending',
  'accepted',
  'started',
  'cancelled',
  'cancelled_by_rider',
  'cancelled_by_owner',
  'cancelled_due_to_missing_payment_proof',
  'finished',
];

/** Posiciones de badge de mercado — deben coincidir con openapi PriceMarketPosition. */
const FRONTEND_PRICE_MARKET_POSITIONS: PriceMarketPosition[] = [
  'below_market',
  'at_market',
  'above_market',
];

describe('openapi.yaml contract (frontend)', () => {
  const yaml = loadOpenApi();

  it('openapi vendor JSON types tienen entrada en MediaTypes del cliente', () => {
    // 1.Arrange
    const inSpec = vendorJsonMediaTypes(yaml);
    const clientValues = new Set(Object.values(MediaTypes));

    // 2.Act
    const missingInClient = [...inSpec].filter(
      (t) => !clientValues.has(t as (typeof MediaTypes)[keyof typeof MediaTypes]),
    );

    // 3.Assert
    expect(
      missingInClient,
      `openapi MIME types missing in client: ${missingInClient.join(', ')}`,
    ).toEqual([]);
  });

  it('MediaTypes del cliente usados en REST están declarados en openapi', () => {
    // 1.Arrange
    const inSpec = vendorJsonMediaTypes(yaml);
    const clientTypes = Object.values(MediaTypes).filter(
      (t) => t.endsWith('+json') && t !== MediaTypes.emailVerificationCode,
    );

    // 2.Act
    const missing = clientTypes.filter((t) => !inSpec.has(t));

    // 3.Assert
    expect(missing, `client MIME types missing in openapi: ${missing.join(', ')}`).toEqual([]);
  });

  it('ReservationStatus del frontend coincide con openapi', () => {
    // 1.Arrange
    const specStatuses = enumValues(yaml, 'ReservationStatus');
    const frontendStatuses = [...FRONTEND_RESERVATION_STATUSES];

    // 2.Act
    frontendStatuses.sort();
    specStatuses.sort();

    // 3.Assert
    expect(frontendStatuses).toEqual(specStatuses);
  });

  it('PriceMarketPosition del frontend coincide con openapi', () => {
    // 1.Arrange
    const specPositions = enumValues(yaml, 'PriceMarketPosition');
    const frontendPositions = [...FRONTEND_PRICE_MARKET_POSITIONS];

    // 2.Act
    frontendPositions.sort();
    specPositions.sort();

    // 3.Assert
    expect(frontendPositions).toEqual(specPositions);
  });

  it('CarDto del frontend cubre las propiedades de openapi', () => {
    // 1.Arrange
    const spec = schemaProperties(yaml, 'CarDto');
    const frontendShape = [
      'plate',
      'year',
      'powertrain',
      'transmission',
      'type',
      'status',
      'description',
      'minimumRentalDays',
      'ratingAvg',
      'dayPrice',
      'brandName',
      'modelName',
      'modelValidated',
      'hasInsurance',
      'priceMarketPositionModifier',
      'marketAveragePrice',
      'marketSampleCount',
      'createdAt',
      'links',
    ];

    // 2.Act
    frontendShape.sort();
    spec.sort();

    // 3.Assert
    expect(frontendShape).toEqual(spec);
  });

  it('ReservationDto del frontend cubre las propiedades de openapi', () => {
    // 1.Arrange
    const spec = schemaProperties(yaml, 'ReservationDto');
    const frontendShape = [
      'startDate',
      'endDate',
      'status',
      'totalPrice',
      'carReturned',
      'paymentProofDeadlineAt',
      'refundProofDeadlineAt',
      'paymentRefundRequired',
      'hasPaymentReceipt',
      'hasRefundReceipt',
      'ownerCbu',
      'pickupStreet',
      'pickupNumber',
      'pickupNeighborhood',
      'checkInTime',
      'checkOutTime',
      'createdAt',
      'links',
    ];

    // 2.Act
    frontendShape.sort();
    spec.sort();

    // 3.Assert
    expect(frontendShape).toEqual(spec);
  });

  it('MessageDto del frontend cubre las propiedades de openapi', () => {
    // 1.Arrange
    const spec = schemaProperties(yaml, 'MessageDto');
    const frontendShape = ['body', 'createdAt', 'seen', 'hasAttachment', 'links'];

    // 2.Act
    frontendShape.sort();
    spec.sort();

    // 3.Assert
    expect(frontendShape).toEqual(spec);
  });

  it('ErrorDto del frontend cubre las propiedades de openapi', () => {
    // 1.Arrange
    const spec = schemaProperties(yaml, 'ErrorDto');
    const frontendShape = ['status', 'code', 'message'];

    // 2.Act
    frontendShape.sort();
    spec.sort();

    // 3.Assert
    expect(frontendShape).toEqual(spec);
  });

  it('ReservationCreateDto requeridos están en el tipo de escritura del cliente', () => {
    // 1.Arrange
    const requiredBlock = yaml.match(
      /^    ReservationCreateDto:\s*\r?\n      type: object\s*\r?\n      required: \[(.+)\]/m,
    );

    // 2.Act
    const required = requiredBlock
      ? requiredBlock[1].split(',').map((s) => s.trim())
      : [];

    // 3.Assert
    expect(requiredBlock).not.toBeNull();
    expect(required).toEqual(['carUri', 'availabilityUri', 'startDate', 'endDate']);
  });
});
