// ************
// Types
// ************

import '@dev.hiconic/gm_root-model';
import '@dev.hiconic/gm_service-api-model';
import '@dev.hiconic/gm_logging-model';
import '@dev.hiconic/gm_transient-resource-model';
import '@dev.hiconic/gm_gm-core-api';

import { T } from '@dev.hiconic/hc-js-base';

export declare namespace meta {
	const groupId: string;
	const artifactId: string;
	const version: string;
}

export import CreateLocalLogBundle = T.hiconic.rx.log.reflection.model.api.CreateLocalLogBundle;
export import CreateLogBundle = T.hiconic.rx.log.reflection.model.api.CreateLogBundle;
export import GetLogTopology = T.hiconic.rx.log.reflection.model.api.GetLogTopology;
export import ListLocalLogStreams = T.hiconic.rx.log.reflection.model.api.ListLocalLogStreams;
export import ListLogStreams = T.hiconic.rx.log.reflection.model.api.ListLogStreams;
export import LogBundleFormat = T.hiconic.rx.log.reflection.model.LogBundleFormat;
export import LogCapability = T.hiconic.rx.log.reflection.model.LogCapability;
export import LogFilter = T.hiconic.rx.log.reflection.model.LogFilter;
export import LogFormat = T.hiconic.rx.log.reflection.model.LogFormat;
export import LogOrigin = T.hiconic.rx.log.reflection.model.LogOrigin;
export import LogParsingQuality = T.hiconic.rx.log.reflection.model.LogParsingQuality;
export import LogPosition = T.hiconic.rx.log.reflection.model.LogPosition;
export import LogPropertyFilter = T.hiconic.rx.log.reflection.model.LogPropertyFilter;
export import LogRecord = T.hiconic.rx.log.reflection.model.LogRecord;
export import LogRecordPage = T.hiconic.rx.log.reflection.model.api.LogRecordPage;
export import LogReflectionRequest = T.hiconic.rx.log.reflection.model.api.LogReflectionRequest;
export import LogSegmentDescriptor = T.hiconic.rx.log.reflection.model.LogSegmentDescriptor;
export import LogSourceLocation = T.hiconic.rx.log.reflection.model.LogSourceLocation;
export import LogStreamDescriptor = T.hiconic.rx.log.reflection.model.LogStreamDescriptor;
export import LogStreamKind = T.hiconic.rx.log.reflection.model.LogStreamKind;
export import LogStreams = T.hiconic.rx.log.reflection.model.api.LogStreams;
export import LogTarget = T.hiconic.rx.log.reflection.model.LogTarget;
export import LogTopology = T.hiconic.rx.log.reflection.model.api.LogTopology;
export import QueryLocalLogRecords = T.hiconic.rx.log.reflection.model.api.QueryLocalLogRecords;
export import QueryLogRecords = T.hiconic.rx.log.reflection.model.api.QueryLogRecords;

declare module '@dev.hiconic/hc-js-base' {

	namespace T.hiconic.rx.log.reflection.model {

		interface LogBundleFormat extends hc.reflection.EnumBase<LogBundleFormat>, hc.Enum<LogBundleFormat> {}
		const LogBundleFormat: {
			readonly [hc.Symbol.enumType]: hc.reflection.EnumType<LogBundleFormat>,
			readonly RAW_FILES: LogBundleFormat,
			readonly CANONICAL_JSONL: LogBundleFormat,
			readonly CANONICAL_TEXT: LogBundleFormat,
		}

		interface LogCapability extends hc.reflection.EnumBase<LogCapability>, hc.Enum<LogCapability> {}
		const LogCapability: {
			readonly [hc.Symbol.enumType]: hc.reflection.EnumType<LogCapability>,
			readonly TIMESTAMP: LogCapability,
			readonly LEVEL: LogCapability,
			readonly LOGGER: LogCapability,
			readonly THREAD: LogCapability,
			readonly SOURCE_LOCATION: LogCapability,
			readonly THROWABLE: LogCapability,
			readonly CUSTOM_PROPERTIES: LogCapability,
			readonly FULLTEXT: LogCapability,
		}

		const LogFilter: hc.reflection.EntityType<LogFilter>;
		type LogFilter = T.com.braintribe.model.generic.GenericEntity &
		  Entity<"hiconic.rx.log.reflection.model.LogFilter", {
			from: date;
			fulltext: string;
			levels: set<T.com.braintribe.model.logging.LogLevel>;
			loggerNameContains: string;
			loggerNames: set<string>;
			propertyFilters: list<LogPropertyFilter>;
			threadNames: set<string>;
			to: date;
		}>;

		interface LogFormat extends hc.reflection.EnumBase<LogFormat>, hc.Enum<LogFormat> {}
		const LogFormat: {
			readonly [hc.Symbol.enumType]: hc.reflection.EnumType<LogFormat>,
			readonly STRUCTURED_EVENT: LogFormat,
			readonly JSON: LogFormat,
			readonly PATTERN: LogFormat,
			readonly RAW: LogFormat,
		}

		const LogOrigin: hc.reflection.EntityType<LogOrigin>;
		type LogOrigin = T.com.braintribe.model.generic.GenericEntity &
		  Entity<"hiconic.rx.log.reflection.model.LogOrigin", {
			applicationId: string;
			nodeId: string;
		}>;

		interface LogParsingQuality extends hc.reflection.EnumBase<LogParsingQuality>, hc.Enum<LogParsingQuality> {}
		const LogParsingQuality: {
			readonly [hc.Symbol.enumType]: hc.reflection.EnumType<LogParsingQuality>,
			readonly EXACT: LogParsingQuality,
			readonly PARTIAL: LogParsingQuality,
			readonly RAW_ONLY: LogParsingQuality,
		}

		const LogPosition: hc.reflection.EntityType<LogPosition>;
		type LogPosition = T.com.braintribe.model.generic.GenericEntity &
		  Entity<"hiconic.rx.log.reflection.model.LogPosition", {
			byteOffset: long;
			sequence: long;
		}>;

		const LogPropertyFilter: hc.reflection.EntityType<LogPropertyFilter>;
		type LogPropertyFilter = T.com.braintribe.model.generic.GenericEntity &
		  Entity<"hiconic.rx.log.reflection.model.LogPropertyFilter", {
			name: string;
			value: string;
		}>;

		const LogRecord: hc.reflection.EntityType<LogRecord>;
		type LogRecord = T.com.braintribe.model.generic.GenericEntity &
		  Entity<"hiconic.rx.log.reflection.model.LogRecord", {
			level: T.com.braintribe.model.logging.LogLevel;
			loggerName: string;
			message: string;
			origin: LogOrigin;
			position: LogPosition;
			properties: map<string, string>;
			rawText: string;
			sourceLocation: LogSourceLocation;
			streamId: string;
			threadName: string;
			throwable: string;
			timestamp: date;
		}>;

		const LogSegmentDescriptor: hc.reflection.EntityType<LogSegmentDescriptor>;
		type LogSegmentDescriptor = T.com.braintribe.model.generic.GenericEntity &
		  Entity<"hiconic.rx.log.reflection.model.LogSegmentDescriptor", {
			active: P<boolean, { nullable: false }>;
			fileName: string;
			lastModified: date;
			segmentId: string;
			size: long;
		}>;

		const LogSourceLocation: hc.reflection.EntityType<LogSourceLocation>;
		type LogSourceLocation = T.com.braintribe.model.generic.GenericEntity &
		  Entity<"hiconic.rx.log.reflection.model.LogSourceLocation", {
			className: string;
			fileName: string;
			lineNumber: integer;
			methodName: string;
		}>;

		const LogStreamDescriptor: hc.reflection.EntityType<LogStreamDescriptor>;
		type LogStreamDescriptor = T.com.braintribe.model.generic.GenericEntity &
		  Entity<"hiconic.rx.log.reflection.model.LogStreamDescriptor", {
			capabilities: set<LogCapability>;
			customFields: set<string>;
			displayName: string;
			format: LogFormat;
			kind: LogStreamKind;
			origin: LogOrigin;
			parsingQuality: LogParsingQuality;
			pattern: string;
			segments: list<LogSegmentDescriptor>;
			streamId: string;
		}>;

		interface LogStreamKind extends hc.reflection.EnumBase<LogStreamKind>, hc.Enum<LogStreamKind> {}
		const LogStreamKind: {
			readonly [hc.Symbol.enumType]: hc.reflection.EnumType<LogStreamKind>,
			readonly FILE: LogStreamKind,
			readonly STRUCTURED_LIVE: LogStreamKind,
		}

		const LogTarget: hc.reflection.EntityType<LogTarget>;
		type LogTarget = T.com.braintribe.model.generic.GenericEntity &
		  Entity<"hiconic.rx.log.reflection.model.LogTarget", {
			applicationId: string;
			nodeId: string;
		}>;

	}

	namespace T.hiconic.rx.log.reflection.model.api {

		const CreateLocalLogBundle: hc.reflection.EntityType<CreateLocalLogBundle>;
		type CreateLocalLogBundle = Evaluable<T.com.braintribe.model.resource.Resource> &
		  LogReflectionRequest &
		  Entity<"hiconic.rx.log.reflection.model.api.CreateLocalLogBundle", {
			filter: T.hiconic.rx.log.reflection.model.LogFilter;
			format: T.hiconic.rx.log.reflection.model.LogBundleFormat;
			includeRotated: P<boolean, { nullable: false }>;
			streamIds: set<string>;
		}>;

		const CreateLogBundle: hc.reflection.EntityType<CreateLogBundle>;
		type CreateLogBundle = Evaluable<T.com.braintribe.model.resource.Resource> &
		  LogReflectionRequest &
		  Entity<"hiconic.rx.log.reflection.model.api.CreateLogBundle", {
			filter: T.hiconic.rx.log.reflection.model.LogFilter;
			format: T.hiconic.rx.log.reflection.model.LogBundleFormat;
			includeRotated: P<boolean, { nullable: false }>;
			streamIds: set<string>;
			target: T.hiconic.rx.log.reflection.model.LogTarget;
		}>;

		const GetLogTopology: hc.reflection.EntityType<GetLogTopology>;
		type GetLogTopology = Evaluable<LogTopology> &
		  LogReflectionRequest &
		  Entity<"hiconic.rx.log.reflection.model.api.GetLogTopology">;

		const ListLocalLogStreams: hc.reflection.EntityType<ListLocalLogStreams>;
		type ListLocalLogStreams = Evaluable<LogStreams> &
		  LogReflectionRequest &
		  Entity<"hiconic.rx.log.reflection.model.api.ListLocalLogStreams">;

		const ListLogStreams: hc.reflection.EntityType<ListLogStreams>;
		type ListLogStreams = Evaluable<LogStreams> &
		  LogReflectionRequest &
		  Entity<"hiconic.rx.log.reflection.model.api.ListLogStreams", {
			target: T.hiconic.rx.log.reflection.model.LogTarget;
		}>;

		const LogRecordPage: hc.reflection.EntityType<LogRecordPage>;
		type LogRecordPage = T.com.braintribe.model.generic.GenericEntity &
		  Entity<"hiconic.rx.log.reflection.model.api.LogRecordPage", {
			errors: map<string, string>;
			moreAvailable: P<boolean, { nullable: false }>;
			nextCursor: string;
			observedProperties: set<string>;
			records: list<T.hiconic.rx.log.reflection.model.LogRecord>;
		}>;

		const LogReflectionRequest: hc.reflection.EntityType<LogReflectionRequest>;
		type LogReflectionRequest = T.com.braintribe.model.service.api.AuthorizedRequest &
		  Entity<"hiconic.rx.log.reflection.model.api.LogReflectionRequest">;

		const LogStreams: hc.reflection.EntityType<LogStreams>;
		type LogStreams = T.com.braintribe.model.generic.GenericEntity &
		  Entity<"hiconic.rx.log.reflection.model.api.LogStreams", {
			errors: map<string, string>;
			streams: list<T.hiconic.rx.log.reflection.model.LogStreamDescriptor>;
		}>;

		const LogTopology: hc.reflection.EntityType<LogTopology>;
		type LogTopology = T.com.braintribe.model.generic.GenericEntity &
		  Entity<"hiconic.rx.log.reflection.model.api.LogTopology", {
			instances: list<T.hiconic.rx.log.reflection.model.LogOrigin>;
			localInstance: T.hiconic.rx.log.reflection.model.LogOrigin;
		}>;

		const QueryLocalLogRecords: hc.reflection.EntityType<QueryLocalLogRecords>;
		type QueryLocalLogRecords = Evaluable<LogRecordPage> &
		  LogReflectionRequest &
		  Entity<"hiconic.rx.log.reflection.model.api.QueryLocalLogRecords", {
			cursors: map<string, string>;
			filter: T.hiconic.rx.log.reflection.model.LogFilter;
			includeRotated: P<boolean, { nullable: false }>;
			limit: P<integer, { nullable: false }>;
			streamId: string;
			waitMillis: P<long, { nullable: false }>;
		}>;

		const QueryLogRecords: hc.reflection.EntityType<QueryLogRecords>;
		type QueryLogRecords = Evaluable<LogRecordPage> &
		  LogReflectionRequest &
		  Entity<"hiconic.rx.log.reflection.model.api.QueryLogRecords", {
			cursor: string;
			filter: T.hiconic.rx.log.reflection.model.LogFilter;
			includeRotated: P<boolean, { nullable: false }>;
			limit: P<integer, { nullable: false }>;
			streamId: string;
			target: T.hiconic.rx.log.reflection.model.LogTarget;
			waitMillis: P<long, { nullable: false }>;
		}>;

	}

}
