import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import { MediaTypes } from './mediaTypes';
import { CLIENT_CONFIG_FALLBACK } from './clientConfig';
import {
  useApiDiscoveryStore,
} from './apiDiscovery';
import { hrefToRelativeApiPath } from './uri';
import type { PriceMarketPosition } from '../components/ryden/car/CarCard';
import { filtersToApiParams } from '../features/browse/searchFilters';
import type { ReservationStatus } from '../features/reservations/types';

const OPENAPI_PATH = resolve(
  dirname(fileURLToPath(import.meta.url)),
  '../../../openapi.yaml',
);
const FRONTEND_SRC_PATH = resolve(dirname(fileURLToPath(import.meta.url)), '..');

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
    const name = line.match(/^        ([\w-]+):/)?.[1];
    if (name) props.push(name);
  }
  return props;
}

function openapiCarSearchQueryParams(yaml: string): string[] {
  const match = yaml.match(
    /  \/cars:\r?\n    get:[\s\S]*?      parameters:\r?\n([\s\S]*?)      responses:/,
  );
  if (!match) throw new Error('GET /cars parameters not found in openapi.yaml');
  const names: string[] = [];
  for (const entry of match[1].split(/\r?\n        - /)) {
    const name = entry.match(/^name: (\w+)/)?.[1];
    if (!name || entry.includes('in: header')) continue;
    names.push(name);
  }
  return names;
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

  it('testOpenApiVendorJsonTypesExistInClientMediaTypes', () => {
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

  it('testClientRestMediaTypesAreDeclaredInOpenApi', () => {
    // 1.Arrange
    const inSpec = vendorJsonMediaTypes(yaml);
    const clientTypes = Object.values(MediaTypes).filter((t) => t.endsWith('+json'));

    // 2.Act
    const missing = clientTypes.filter((t) => !inSpec.has(t));

    // 3.Assert
    expect(missing, `client MIME types missing in openapi: ${missing.join(', ')}`).toEqual([]);
  });

  it('testAdminUsersCollectionUsesUserPrivateMime', () => {
    // 1.Arrange — la lista admin GET /users devuelve la vista privada (UserPrivateDto).
    const usersGetBlock =
      yaml.match(/^ {2}\/users:\r?\n {4}get:\r?\n([\s\S]*?)^ {4}post:/m)?.[1] ?? '';

    // 2.Act
    const declaresPrivateList =
      usersGetBlock.includes(MediaTypes.userPrivate) &&
      usersGetBlock.includes('items: { $ref: "#/components/schemas/UserPrivateDto" }');
    const declaresPublicList = usersGetBlock.includes(
      'items: { $ref: "#/components/schemas/UserDto" }',
    );

    // 3.Assert
    expect(usersGetBlock, 'GET /users missing in openapi.yaml').not.toBe('');
    expect(declaresPrivateList).toBe(true);
    expect(declaresPublicList).toBe(false);
  });

  it('testReservationStatusMatchesOpenApi', () => {
    // 1.Arrange
    const specStatuses = enumValues(yaml, 'ReservationStatus');
    const frontendStatuses = [...FRONTEND_RESERVATION_STATUSES];

    // 2.Act
    frontendStatuses.sort();
    specStatuses.sort();

    // 3.Assert
    expect(frontendStatuses).toEqual(specStatuses);
  });

  it('testPriceMarketPositionMatchesOpenApi', () => {
    // 1.Arrange
    const specPositions = enumValues(yaml, 'PriceMarketPosition');
    const frontendPositions = [...FRONTEND_PRICE_MARKET_POSITIONS];

    // 2.Act
    frontendPositions.sort();
    specPositions.sort();

    // 3.Assert
    expect(frontendPositions).toEqual(specPositions);
  });

  it('testCarSummaryDtoShapeMatchesOpenApi', () => {
    // 1.Arrange
    const spec = schemaProperties(yaml, 'CarSummaryDto');
    const frontendShape = [
      'brandName',
      'modelName',
      'year',
      'status',
      'minimumRentalDays',
      'ratingAvg',
      'dayPrice',
      'modelValidated',
      'priceMarketPositionModifier',
      'marketAveragePrice',
      'marketSampleCount',
      'links',
    ];

    // 2.Act
    frontendShape.sort();
    spec.sort();

    // 3.Assert
    expect(frontendShape).toEqual(spec);
  });

  it('testCarDtoShapeMatchesOpenApi', () => {
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

  it('testReservationSummaryDtoShapeMatchesOpenApi', () => {
    // 1.Arrange
    const spec = schemaProperties(yaml, 'ReservationSummaryDto');
    const frontendShape = ['startDate', 'endDate', 'status', 'totalPrice', 'brandName', 'modelName', 'links'];

    // 2.Act
    frontendShape.sort();
    spec.sort();

    // 3.Assert
    expect(frontendShape).toEqual(spec);
  });

  it('testReservationDtoShapeMatchesOpenApi', () => {
    // 1.Arrange
    const spec = schemaProperties(yaml, 'ReservationDto');
    const frontendShape = [
      'startDate',
      'endDate',
      'status',
      'totalPrice',
      'carReturned',
      'carReturnedAt',
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

  it('testMessageDtoShapeMatchesOpenApi', () => {
    // 1.Arrange
    const spec = schemaProperties(yaml, 'MessageDto');
    const frontendShape = ['attachment', 'body', 'createdAt', 'hasAttachment', 'links', 'seen'];

    // 2.Act
    frontendShape.sort();
    spec.sort();

    // 3.Assert
    expect(frontendShape).toEqual(spec);
  });

  it('testErrorDtoShapeMatchesOpenApi', () => {
    // 1.Arrange
    const spec = schemaProperties(yaml, 'ErrorDto');
    const frontendShape = ['status', 'code', 'message'];

    // 2.Act
    frontendShape.sort();
    spec.sort();

    // 3.Assert
    expect(frontendShape).toEqual(spec);
  });

  it('testReservationCreateDtoRequiredFieldsMatchOpenApi', () => {
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

  it('testFiltersToApiParamsOnlyEmitOpenApiCarSearchQueryParams', () => {
    // 1.Arrange
    const specParams = new Set(openapiCarSearchQueryParams(yaml));
    const sample = filtersToApiParams({
      q: 'corolla',
      category: ['sedan'],
      transmission: ['automatic'],
      powertrain: ['hybrid', 'gasoline'],
      priceMin: 10,
      priceMax: 50,
      priceMarket: ['below_market'],
      rating: ['4', '5'],
      neighborhoodIds: [7, 8],
      flexible: true,
      flexMonth: '2026-07',
      flexDays: 5,
      sort: 'price_asc',
    });

    // 2.Act
    const unknown = Object.keys(sample).filter((key) => !specParams.has(key));

    // 3.Assert
    expect(unknown, `query params not in openapi GET /cars: ${unknown.join(', ')}`).toEqual([]);
  });

  it('testSprintBOpenApiDocumentsHypermediaAndCollectionSemantics', () => {
    // 1.Arrange
    const bookableFields = schemaProperties(yaml, 'BookableSegmentDto');
    const publicUserFields = schemaProperties(yaml, 'UserDto');

    // 2.Act
    const reviewCreateUsesReservationUri =
      yaml.includes('required: [reservationUri]') &&
      yaml.includes('description: URN canónica de la reserva (`links.self`).');

    // 3.Assert
    expect(bookableFields).toContain('links');
    expect(bookableFields).not.toContain('neighborhoodId');
    expect(publicUserFields).not.toContain('licenseValidated');
    expect(publicUserFields).not.toContain('identityUploaded');
    expect(reviewCreateUsesReservationUri).toBe(true);
    expect(yaml).toContain('El catálogo admin se solicita con `status=all`.');
    expect(yaml).toContain('/neighborhoods/{id}:');
  });

  it('testRuntimeOptionalPathsDocumentedInOpenApi', () => {
    // 1.Arrange / 2.Act — T-06: paths used at runtime that needed contract coverage
    // 3.Assert
    expect(yaml).toContain('/cars/{id}/bookable-segments:');
    expect(yaml).toContain('/reservations/{id}/counterparty:');
    expect(yaml).toContain('/brands/{id}/models/{modelId}/price-insight:');
    expect(yaml).toContain(MediaTypes.bookableSegment);
    expect(yaml).toContain(MediaTypes.counterpartyContact);
    expect(yaml).toContain(MediaTypes.priceMarketInsight);
  });

  it('testClientConfigFallbackKeysMatchOpenApiSchema', () => {
    // 1.Arrange
    const topLevel = schemaProperties(yaml, 'ClientConfig');
    // Hypermedia `links` is runtime-only; the SPA fallback mirrors policy fields, not HATEOAS.
    const policyTopLevel = topLevel.filter((k) => k !== 'links');
    expect(topLevel).toContain('links');
    const carKeys = [
      'brandMinLength',
      'brandMaxLength',
      'modelMaxLength',
      'plateMinLength',
      'plateMaxLength',
      'descriptionMaxLength',
      'yearMin',
      'galleryMaxItems',
    ];
    const uploadKeys = [
      'maxImageMegabytes',
      'maxCarGalleryVideoMegabytes',
      'maxProfileDocumentMegabytes',
      'maxPaymentReceiptMegabytes',
    ];
    const moneyKeys = ['currency', 'formatLocale'];
    const userKeys = [
      'displayNamePartMaxLength',
      'profileAboutMaxLength',
      'registrationPasswordMinLength',
      'registrationPasswordMaxLength',
      'registrationEmailMaxLength',
      'profilePhoneMaxLength',
    ];
    const chatKeys = ['maxAttachmentMegabytes', 'messageMaxLength', 'historyPageSize'];
    const reviewKeys = ['commentMaxLength'];
    const listingKeys = [
      'pricePerDayMin',
      'addressStreetMaxLength',
      'addressNumberMaxLength',
      'pricePerDayIntegerDigits',
      'pricePerDayFractionDigits',
    ];

    // 2.Act / 3.Assert
    expect(Object.keys(CLIENT_CONFIG_FALLBACK).sort()).toEqual(policyTopLevel.sort());
    expect(Object.keys(CLIENT_CONFIG_FALLBACK.car).sort()).toEqual(carKeys.sort());
    expect(Object.keys(CLIENT_CONFIG_FALLBACK.upload).sort()).toEqual(uploadKeys.sort());
    expect(Object.keys(CLIENT_CONFIG_FALLBACK.money).sort()).toEqual(moneyKeys.sort());
    expect(Object.keys(CLIENT_CONFIG_FALLBACK.user).sort()).toEqual(userKeys.sort());
    expect(Object.keys(CLIENT_CONFIG_FALLBACK.chat).sort()).toEqual(chatKeys.sort());
    expect(Object.keys(CLIENT_CONFIG_FALLBACK.review).sort()).toEqual(reviewKeys.sort());
    expect(Object.keys(CLIENT_CONFIG_FALLBACK.listing).sort()).toEqual(listingKeys.sort());
  });

  it('testApiIndexSchemaPropertiesMatchFrontendDiscoveryDto', () => {
    // 1.Arrange
    const specProps = schemaProperties(yaml, 'ApiIndex').sort();

    // 2.Act
    const clientShape = ['links', 'resources'].sort();

    // 3.Assert
    expect(clientShape).toEqual(specProps);
  });

  it('testApiIndexResourceDescriptorSchemaMatchesFrontendType', () => {
    // 1.Arrange
    const specProps = schemaProperties(yaml, 'ApiIndexResourceDescriptor').sort();

    // 2.Act
    const clientShape = ['href', 'itemTemplate', 'queryParams'].sort();

    // 3.Assert
    expect(clientShape).toEqual(specProps);
  });

  it('testUserPrivateDtoSchemaCoversProfileUserDtoPrivateFields', () => {
    // 1.Arrange
    const specProps = new Set(schemaProperties(yaml, 'UserPrivateDto'));
    const profilePrivateFields = [
      'forename',
      'surname',
      'email',
      'phoneNumber',
      'birthDate',
      'about',
      'cbu',
      'memberSince',
      'latestLocale',
      'emailVerified',
      'licenseValidated',
      'identityValidated',
      'licenseUploaded',
      'identityUploaded',
      'hasProfilePicture',
      'blocked',
      'role',
      'ratingAsRider',
      'ratingAsOwner',
      'links',
    ];

    // 2.Act
    const missing = profilePrivateFields.filter((field) => !specProps.has(field));

    // 3.Assert
    expect(missing, `UserPrivateDto missing in openapi: ${missing.join(', ')}`).toEqual([]);
  });

  it('testApiDiscoveryCollectionPathRequiresIndexHref', () => {
    // 1.Arrange
    useApiDiscoveryStore.setState({
      ready: false,
      index: null,
    });

    // 2.Act / 3.Assert
    expect(() => useApiDiscoveryStore.getState().collectionPath('cars')).toThrow(
      'api.discovery.missingCollection:cars',
    );
  });

  it('testApiDiscoveryCollectionPathPrefersIndexResourceHref', () => {
    // 1.Arrange
    const href = 'http://localhost/webapp/api/cars';
    useApiDiscoveryStore.setState({
      ready: true,
      index: {
        links: { self: 'http://localhost/webapp/api/' },
        resources: {
          cars: { href, queryParams: ['page'] },
        },
      },
    });

    // 2.Act
    const path = useApiDiscoveryStore.getState().collectionPath('cars');

    // 3.Assert
    expect(path).toBe(hrefToRelativeApiPath(href));
  });

  it('testLinksSchemaDocumentsTypedUserDocumentRels', () => {
    // 1.Arrange
    const linkProps = schemaProperties(yaml, 'Links');

    // 2.Act / 3.Assert
    expect(linkProps).toContain('self');
    expect(linkProps).toContain('identityDocument');
    expect(linkProps).toContain('licenseDocument');
    expect(linkProps).toContain('blocked-overdue-reservation');
  });

  it('testFeatureApisDoNotFabricateUserDocumentUrls', () => {
    // 1.Arrange
    const apiFiles = ['profile', 'owner', 'admin', 'reservations']
      .map((feature) => resolve(FRONTEND_SRC_PATH, `features/${feature}/api.ts`));

    // 2.Act
    const sources = apiFiles.map((path) => readFileSync(path, 'utf8'));

    // 3.Assert
    for (const source of sources) {
      expect(source).not.toMatch(/links\??\.documents/);
      expect(source).not.toMatch(/\$\{[^}\n]+\}\/documents(?:\/|`)/);
      expect(source).not.toMatch(/subResource\([^)\n]*['"]documents['"]\)/);
    }
  });

  it('testClientConfigChatLimitsAreDocumentedInOpenApi', () => {
    // 1.Arrange
    const chatBlock = yaml.match(
      /        chat:\r?\n          type: object\r?\n          description:[\s\S]*?          properties:\r?\n([\s\S]*?)          required: \[maxAttachmentMegabytes, messageMaxLength, historyPageSize\]/,
    );

    // 2.Act
    const documented = chatBlock?.[1] ?? '';

    // 3.Assert
    expect(chatBlock, 'ClientConfig.chat block missing in openapi.yaml').not.toBeNull();
    expect(documented).toContain('maxAttachmentMegabytes');
    expect(documented).toContain('messageMaxLength');
    expect(documented).toContain('historyPageSize');
  });
});
