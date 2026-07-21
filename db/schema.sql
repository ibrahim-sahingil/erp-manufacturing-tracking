--
-- PostgreSQL database dump
--

\restrict decTJE1mEtHFzncHa51wE2CfaB2rgLghmsP3L86tHen6ayR2aUog9T1fLR7XVe8

-- Dumped from database version 18.3
-- Dumped by pg_dump version 18.3

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


--
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: bom_document_parts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bom_document_parts (
    document_id uuid NOT NULL,
    bom_part_id uuid NOT NULL
);


--
-- Name: bom_documents; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bom_documents (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    product_id uuid NOT NULL,
    category character varying(20) NOT NULL,
    filename character varying(255) NOT NULL,
    content_type character varying(100),
    size_bytes bigint NOT NULL,
    data bytea NOT NULL,
    uploaded_by character varying(150),
    created_at timestamp without time zone DEFAULT now(),
    CONSTRAINT bom_documents_category_chk CHECK (((category)::text = ANY ((ARRAY['URETIM'::character varying, 'ARGE'::character varying])::text[])))
);


--
-- Name: bom_operations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bom_operations (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    name character varying(150) NOT NULL,
    code character varying(50) NOT NULL,
    description text,
    sort_order integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    department_name character varying(100)
);


--
-- Name: COLUMN bom_operations.department_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.bom_operations.department_name IS 'Bu islemin yapildigi bolum adi (or. Kaynak). Projeye uygulanirken ayni isimli departments kaydi bulunur/olusturulur.';


--
-- Name: bom_parts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bom_parts (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    product_id uuid NOT NULL,
    parent_id uuid,
    name character varying(200) NOT NULL,
    code character varying(100) NOT NULL,
    quantity numeric(15,4) DEFAULT 1,
    unit character varying(20) DEFAULT 'adet'::character varying,
    weight_kg numeric(15,4),
    material character varying(150),
    operations jsonb DEFAULT '[]'::jsonb,
    level integer DEFAULT 0,
    sort_order integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    width_mm numeric(15,4),
    height_mm numeric(15,4),
    thickness_mm numeric(15,4),
    material_kind character varying(20),
    length_mm numeric(15,4),
    diameter_mm numeric(15,4),
    material_form character varying(20),
    department_name character varying(100),
    CONSTRAINT bom_parts_material_form_chk CHECK (((material_form IS NULL) OR ((material_form)::text = ANY ((ARRAY['SAC'::character varying, 'PROFIL'::character varying, 'MIL'::character varying, 'BORU'::character varying, 'DELRIN'::character varying, 'COK_KOMPONENTLI'::character varying])::text[])))),
    CONSTRAINT bom_parts_material_kind_chk CHECK (((material_kind IS NULL) OR ((material_kind)::text = ANY ((ARRAY['TEDARIK'::character varying, 'HAMMADDE'::character varying, 'YARI_MAMUL'::character varying, 'MAMUL'::character varying, 'SARF'::character varying])::text[]))))
);


--
-- Name: bom_products; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bom_products (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    name character varying(200) NOT NULL,
    code character varying(100),
    unit character varying(20) DEFAULT 'adet'::character varying,
    description text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: carriers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.carriers (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(150) NOT NULL,
    contact_person character varying(150),
    phone character varying(50),
    email character varying(150),
    address character varying(300),
    tax_office character varying(100),
    tax_number character varying(50),
    notes text,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: company_settings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.company_settings (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(200) DEFAULT ''::character varying NOT NULL,
    address text,
    phone character varying(50),
    email character varying(150),
    tax_office character varying(100),
    tax_number character varying(50),
    logo bytea,
    logo_content_type character varying(100),
    updated_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: delivery_note_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.delivery_note_items (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    delivery_note_id uuid NOT NULL,
    warehouse_id uuid,
    item_name character varying(200) NOT NULL,
    item_code character varying(100),
    quantity numeric(15,4) DEFAULT 1 NOT NULL,
    unit character varying(20) DEFAULT 'adet'::character varying,
    notes character varying(300),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT delivery_note_items_qty_check CHECK ((quantity > (0)::numeric))
);


--
-- Name: delivery_notes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.delivery_notes (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    note_no character varying(30) NOT NULL,
    order_id uuid,
    recipient_name character varying(200) NOT NULL,
    tax_number character varying(20),
    tax_office character varying(100),
    address text,
    city character varying(50),
    district character varying(50),
    scenario character varying(30) DEFAULT 'TEMEL'::character varying,
    note_type character varying(30) DEFAULT 'SEVK'::character varying,
    carrier character varying(150),
    status character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
    ship_date date,
    notes text,
    created_by character varying(100),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    shipped_at timestamp without time zone,
    vehicle_plate character varying(20),
    driver_name character varying(150),
    container_no character varying(50),
    tir_no character varying(50),
    cargo_tracking_no character varying(100),
    eta_date date,
    delivery_terms character varying(100),
    origin_country character varying(100),
    CONSTRAINT delivery_notes_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'SHIPPED'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: departments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.departments (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    order_id uuid,
    name character varying(100) NOT NULL,
    sort_order integer DEFAULT 1,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: materials; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.materials (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(150) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now()
);


--
-- Name: order_documents; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.order_documents (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    order_id uuid NOT NULL,
    category character varying(20) DEFAULT 'QUOTE'::character varying NOT NULL,
    filename character varying(300) NOT NULL,
    content_type character varying(150),
    size_bytes bigint,
    data bytea NOT NULL,
    uploaded_by character varying(150),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT order_documents_category_check CHECK (((category)::text = ANY ((ARRAY['QUOTE'::character varying, 'ORDER'::character varying])::text[])))
);


--
-- Name: order_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.order_items (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    order_id uuid NOT NULL,
    item_name character varying(150) NOT NULL,
    description text,
    quantity integer DEFAULT 1,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: orders; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.orders (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    project_name character varying(100) NOT NULL,
    customer_name character varying(150),
    customer_email character varying(100),
    customer_phone character varying(50),
    location character varying(150),
    delivery_days integer,
    total_price numeric(15,2),
    currency character varying(10) DEFAULT 'TRY'::character varying,
    status character varying(20) DEFAULT 'active'::character varying,
    approved_by uuid,
    notes text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    approved_at timestamp without time zone,
    approval_note text,
    shipping_status character varying(20),
    CONSTRAINT orders_shipping_status_chk CHECK (((shipping_status IS NULL) OR ((shipping_status)::text = ANY ((ARRAY['hazirlaniyor'::character varying, 'yuklendi'::character varying, 'sevk_edildi'::character varying, 'teslim_edildi'::character varying])::text[])))),
    CONSTRAINT orders_status_chk CHECK (((status)::text = ANY ((ARRAY['quote'::character varying, 'quote_lost'::character varying, 'active'::character varying, 'pending'::character varying, 'completed'::character varying, 'cancelled'::character varying])::text[])))
);


--
-- Name: part_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.part_logs (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    part_id uuid NOT NULL,
    user_id uuid NOT NULL,
    qty_done integer DEFAULT 0,
    qty_pending integer DEFAULT 0,
    qty_reject integer DEFAULT 0,
    note text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: parts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.parts (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    order_id uuid,
    department_id uuid,
    name character varying(150) NOT NULL,
    code character varying(100) NOT NULL,
    drawing_no character varying(100),
    material character varying(100),
    total_qty integer DEFAULT 1,
    status character varying(20) DEFAULT 'PENDING'::character varying,
    description text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    qty_done integer DEFAULT 0 NOT NULL,
    qty_pending integer DEFAULT 0 NOT NULL,
    qty_reject integer DEFAULT 0 NOT NULL,
    parent_part_id uuid,
    CONSTRAINT parts_qty_done_nonneg CHECK ((qty_done >= 0)),
    CONSTRAINT parts_qty_pending_nonneg CHECK ((qty_pending >= 0)),
    CONSTRAINT parts_qty_reject_nonneg CHECK ((qty_reject >= 0))
);


--
-- Name: project_bom; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_bom (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    project_name character varying(100) NOT NULL,
    bom_product_id uuid NOT NULL,
    status character varying(20) DEFAULT 'draft'::character varying,
    created_by character varying(150),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    published_at timestamp without time zone,
    product_qty integer DEFAULT 1 NOT NULL
);


--
-- Name: project_bom_parts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_bom_parts (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    project_bom_id uuid NOT NULL,
    bom_part_id uuid,
    is_excluded boolean DEFAULT false,
    custom_name character varying(200),
    custom_code character varying(100),
    custom_qty numeric(15,4) DEFAULT 1,
    custom_unit character varying(20),
    custom_weight numeric(15,4),
    custom_material character varying(150),
    dept_id uuid,
    parent_custom_id uuid,
    operations jsonb DEFAULT '[]'::jsonb,
    level integer DEFAULT 0,
    sort_order integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    custom_width_mm numeric(15,4),
    custom_height_mm numeric(15,4),
    custom_thickness_mm numeric(15,4),
    material_kind character varying(20),
    custom_length_mm numeric(15,4),
    custom_diameter_mm numeric(15,4),
    material_form character varying(20),
    procurement_decision character varying(10),
    decided_by character varying(150),
    decided_at timestamp without time zone,
    ship_planned boolean DEFAULT false NOT NULL,
    ship_planned_qty numeric(15,4),
    CONSTRAINT project_bom_parts_material_form_chk CHECK (((material_form IS NULL) OR ((material_form)::text = ANY ((ARRAY['SAC'::character varying, 'PROFIL'::character varying, 'MIL'::character varying, 'BORU'::character varying, 'DELRIN'::character varying, 'COK_KOMPONENTLI'::character varying])::text[])))),
    CONSTRAINT project_bom_parts_material_kind_chk CHECK (((material_kind IS NULL) OR ((material_kind)::text = ANY ((ARRAY['TEDARIK'::character varying, 'HAMMADDE'::character varying, 'YARI_MAMUL'::character varying, 'MAMUL'::character varying, 'SARF'::character varying])::text[])))),
    CONSTRAINT project_bom_parts_proc_decision_chk CHECK (((procurement_decision IS NULL) OR ((procurement_decision)::text = ANY (ARRAY[('PURCHASE'::character varying)::text, ('PRODUCE'::character varying)::text, ('POOL'::character varying)::text]))))
);


--
-- Name: project_date_revisions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_date_revisions (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    project_date_id uuid NOT NULL,
    old_start date,
    old_end date,
    new_start date NOT NULL,
    new_end date NOT NULL,
    reason text NOT NULL,
    revised_by uuid,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: project_dates; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_dates (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    order_id uuid NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: purchase_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.purchase_items (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    project_name character varying(100) NOT NULL,
    project_bom_part_id uuid,
    name character varying(200) NOT NULL,
    code character varying(100),
    quantity numeric(15,4) DEFAULT 1,
    unit character varying(20) DEFAULT 'adet'::character varying,
    material character varying(150),
    supplier character varying(150),
    unit_price numeric(15,2),
    currency character varying(10) DEFAULT 'TRY'::character varying,
    expected_date date,
    status character varying(20) DEFAULT 'PLANNED'::character varying,
    notes text,
    ordered_at timestamp without time zone,
    received_at timestamp without time zone,
    created_by character varying(150),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    warehouse_id uuid,
    purchase_order_id uuid,
    needs_planning boolean DEFAULT false NOT NULL,
    stock_plan_id uuid,
    received_by character varying(150),
    received_qty numeric(15,4) DEFAULT 0 NOT NULL,
    returned_qty numeric(15,4) DEFAULT 0 NOT NULL,
    sent_to_purchasing boolean DEFAULT true NOT NULL,
    CONSTRAINT purchase_items_status_check CHECK (((status)::text = ANY ((ARRAY['PLANNED'::character varying, 'ORDERED'::character varying, 'RECEIVED'::character varying, 'IN_WAREHOUSE'::character varying, 'IN_STOCK'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: purchase_order_quotes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.purchase_order_quotes (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    purchase_order_id uuid NOT NULL,
    supplier_name character varying(150) NOT NULL,
    contact_info character varying(200),
    total_price numeric(15,2),
    currency character varying(10) DEFAULT 'TRY'::character varying,
    delivery_date date,
    notes text,
    rejection_reason text,
    created_at timestamp without time zone DEFAULT now()
);


--
-- Name: purchase_orders; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.purchase_orders (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(200) NOT NULL,
    status character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
    selected_quote_id uuid,
    approval_note text,
    approved_by character varying(150),
    approved_at timestamp without time zone,
    ordered_at timestamp without time zone,
    created_by character varying(150),
    created_at timestamp without time zone DEFAULT now(),
    code character varying(30) NOT NULL,
    CONSTRAINT purchase_orders_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'APPROVED'::character varying, 'ORDERED'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: shipment_package_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.shipment_package_items (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    package_id uuid NOT NULL,
    part_id uuid,
    project_bom_part_id uuid,
    item_name character varying(200) NOT NULL,
    item_code character varying(100),
    quantity numeric(15,4) NOT NULL,
    unit character varying(20) DEFAULT 'adet'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT shipment_package_items_quantity_check CHECK ((quantity > (0)::numeric))
);


--
-- Name: shipment_packages; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.shipment_packages (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    package_no character varying(30) NOT NULL,
    project_name character varying(100) NOT NULL,
    name character varying(150),
    length_cm numeric(10,2),
    width_cm numeric(10,2),
    height_cm numeric(10,2),
    weight_kg numeric(12,3),
    status character varying(20) DEFAULT 'OPEN'::character varying NOT NULL,
    delivery_note_id uuid,
    packed_by character varying(150),
    packed_at timestamp without time zone,
    notes text,
    created_by character varying(150),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    warehouse_id uuid,
    package_type character varying(30) DEFAULT 'PACKAGE'::character varying NOT NULL,
    net_weight_kg numeric(12,3),
    CONSTRAINT shipment_packages_status_chk CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying, 'CLOSED'::character varying, 'LOADED'::character varying, 'SHIPPED'::character varying])::text[]))),
    CONSTRAINT shipment_packages_type_chk CHECK (((package_type)::text = ANY ((ARRAY['PACKAGE'::character varying, 'BOX'::character varying, 'PALLET'::character varying, 'CRATE'::character varying])::text[])))
);


--
-- Name: stock_sheets; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stock_sheets (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    kind character varying(10) NOT NULL,
    name character varying(150) NOT NULL,
    material character varying(150),
    width_mm numeric(15,4),
    height_mm numeric(15,4),
    thickness_mm numeric(15,4),
    length_mm numeric(15,4),
    notes text,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now(),
    CONSTRAINT stock_sheets_kind_chk CHECK (((kind)::text = ANY ((ARRAY['SAC'::character varying, 'PROFIL'::character varying])::text[])))
);


--
-- Name: suppliers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.suppliers (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(150) NOT NULL,
    contact_person character varying(150),
    phone character varying(50),
    email character varying(150),
    address character varying(300),
    tax_office character varying(100),
    tax_number character varying(50),
    notes text,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now()
);


--
-- Name: user_pins; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_pins (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    user_id uuid NOT NULL,
    pin_type character varying(50) NOT NULL,
    pin_key character varying(100) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    full_name character varying(100) NOT NULL,
    department character varying(50),
    role character varying(20) NOT NULL,
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    username character varying,
    password_hash character varying(255),
    pin_code character varying,
    permissions jsonb DEFAULT '[]'::jsonb
);


--
-- Name: warehouse_movements; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.warehouse_movements (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    warehouse_id uuid NOT NULL,
    purchase_item_id uuid,
    item_name character varying(200) NOT NULL,
    item_code character varying(100),
    movement_type character varying(10) NOT NULL,
    quantity numeric(15,4) NOT NULL,
    unit character varying(20) DEFAULT 'adet'::character varying,
    source_type character varying(30) DEFAULT 'MANUAL'::character varying NOT NULL,
    performed_by character varying(150),
    notes text,
    created_at timestamp without time zone DEFAULT now(),
    delivery_note_id uuid,
    reservation_id uuid,
    shipment_package_id uuid,
    CONSTRAINT warehouse_movements_quantity_check CHECK ((quantity > (0)::numeric)),
    CONSTRAINT warehouse_movements_source_check CHECK (((source_type)::text = ANY ((ARRAY['MANUAL'::character varying, 'PURCHASE_TRANSFER'::character varying, 'GOODS_RECEIPT'::character varying, 'DELIVERY'::character varying, 'WAREHOUSE_TRANSFER'::character varying, 'RESERVATION'::character varying, 'RESERVATION_ADJUST'::character varying, 'PACKAGE'::character varying])::text[]))),
    CONSTRAINT warehouse_movements_type_check CHECK (((movement_type)::text = ANY ((ARRAY['IN'::character varying, 'OUT'::character varying])::text[])))
);


--
-- Name: warehouse_reservations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.warehouse_reservations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    project_name character varying(100) NOT NULL,
    warehouse_id uuid NOT NULL,
    item_name character varying(200) NOT NULL,
    item_code character varying(100),
    requested_qty numeric(15,4) NOT NULL,
    approved_qty numeric(15,4),
    unit character varying(20) DEFAULT 'adet'::character varying,
    status character varying(20) DEFAULT 'REQUESTED'::character varying NOT NULL,
    shortage_reason text,
    requested_by character varying(150),
    approved_by character varying(150),
    approved_at timestamp without time zone,
    notes text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    target_warehouse_id uuid,
    CONSTRAINT warehouse_reservations_requested_qty_check CHECK ((requested_qty > (0)::numeric)),
    CONSTRAINT warehouse_reservations_status_check CHECK (((status)::text = ANY ((ARRAY['REQUESTED'::character varying, 'APPROVED'::character varying, 'PARTIAL'::character varying, 'REJECTED'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: warehouses; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.warehouses (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(150) NOT NULL,
    location character varying(200),
    responsible_user_id uuid,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now()
);


--
-- Name: work_order_parts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.work_order_parts (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    work_order_id uuid NOT NULL,
    part_id uuid NOT NULL,
    qty integer DEFAULT 1,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: work_order_revisions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.work_order_revisions (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    work_order_id uuid NOT NULL,
    field_changed character varying(100) NOT NULL,
    old_value text,
    new_value text,
    reason text NOT NULL,
    revised_by uuid,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: work_orders; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.work_orders (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    order_id uuid NOT NULL,
    department_id uuid,
    workspace_id uuid,
    assigned_user_id uuid,
    start_datetime timestamp without time zone NOT NULL,
    end_datetime timestamp without time zone,
    status character varying(20) DEFAULT 'planned'::character varying,
    notes text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    code character varying(20)
);


--
-- Name: workspace_members; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.workspace_members (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    workspace_id uuid NOT NULL,
    user_id uuid NOT NULL,
    role character varying(100),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: workspaces; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.workspaces (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    name character varying(150) NOT NULL,
    type character varying(50) DEFAULT 'area'::character varying,
    description text,
    sort_order integer DEFAULT 1,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: bom_document_parts bom_document_parts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bom_document_parts
    ADD CONSTRAINT bom_document_parts_pkey PRIMARY KEY (document_id, bom_part_id);


--
-- Name: bom_documents bom_documents_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bom_documents
    ADD CONSTRAINT bom_documents_pkey PRIMARY KEY (id);


--
-- Name: bom_operations bom_operations_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bom_operations
    ADD CONSTRAINT bom_operations_code_key UNIQUE (code);


--
-- Name: bom_operations bom_operations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bom_operations
    ADD CONSTRAINT bom_operations_pkey PRIMARY KEY (id);


--
-- Name: bom_parts bom_parts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bom_parts
    ADD CONSTRAINT bom_parts_pkey PRIMARY KEY (id);


--
-- Name: bom_products bom_products_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bom_products
    ADD CONSTRAINT bom_products_pkey PRIMARY KEY (id);


--
-- Name: carriers carriers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carriers
    ADD CONSTRAINT carriers_pkey PRIMARY KEY (id);


--
-- Name: company_settings company_settings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_settings
    ADD CONSTRAINT company_settings_pkey PRIMARY KEY (id);


--
-- Name: delivery_note_items delivery_note_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.delivery_note_items
    ADD CONSTRAINT delivery_note_items_pkey PRIMARY KEY (id);


--
-- Name: delivery_notes delivery_notes_note_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.delivery_notes
    ADD CONSTRAINT delivery_notes_note_no_key UNIQUE (note_no);


--
-- Name: delivery_notes delivery_notes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.delivery_notes
    ADD CONSTRAINT delivery_notes_pkey PRIMARY KEY (id);


--
-- Name: departments departments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.departments
    ADD CONSTRAINT departments_pkey PRIMARY KEY (id);


--
-- Name: materials materials_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.materials
    ADD CONSTRAINT materials_pkey PRIMARY KEY (id);


--
-- Name: order_documents order_documents_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_documents
    ADD CONSTRAINT order_documents_pkey PRIMARY KEY (id);


--
-- Name: order_items order_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT order_items_pkey PRIMARY KEY (id);


--
-- Name: orders orders_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (id);


--
-- Name: orders orders_project_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_project_name_key UNIQUE (project_name);


--
-- Name: part_logs part_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.part_logs
    ADD CONSTRAINT part_logs_pkey PRIMARY KEY (id);


--
-- Name: parts parts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.parts
    ADD CONSTRAINT parts_pkey PRIMARY KEY (id);


--
-- Name: project_bom_parts project_bom_parts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_bom_parts
    ADD CONSTRAINT project_bom_parts_pkey PRIMARY KEY (id);


--
-- Name: project_bom project_bom_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_bom
    ADD CONSTRAINT project_bom_pkey PRIMARY KEY (id);


--
-- Name: project_bom project_bom_project_product_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_bom
    ADD CONSTRAINT project_bom_project_product_unique UNIQUE (project_name, bom_product_id);


--
-- Name: project_date_revisions project_date_revisions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_date_revisions
    ADD CONSTRAINT project_date_revisions_pkey PRIMARY KEY (id);


--
-- Name: project_dates project_dates_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_dates
    ADD CONSTRAINT project_dates_pkey PRIMARY KEY (id);


--
-- Name: purchase_items purchase_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.purchase_items
    ADD CONSTRAINT purchase_items_pkey PRIMARY KEY (id);


--
-- Name: purchase_order_quotes purchase_order_quotes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.purchase_order_quotes
    ADD CONSTRAINT purchase_order_quotes_pkey PRIMARY KEY (id);


--
-- Name: purchase_orders purchase_orders_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.purchase_orders
    ADD CONSTRAINT purchase_orders_code_key UNIQUE (code);


--
-- Name: purchase_orders purchase_orders_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.purchase_orders
    ADD CONSTRAINT purchase_orders_pkey PRIMARY KEY (id);


--
-- Name: shipment_package_items shipment_package_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shipment_package_items
    ADD CONSTRAINT shipment_package_items_pkey PRIMARY KEY (id);


--
-- Name: shipment_packages shipment_packages_package_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shipment_packages
    ADD CONSTRAINT shipment_packages_package_no_key UNIQUE (package_no);


--
-- Name: shipment_packages shipment_packages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shipment_packages
    ADD CONSTRAINT shipment_packages_pkey PRIMARY KEY (id);


--
-- Name: stock_sheets stock_sheets_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_sheets
    ADD CONSTRAINT stock_sheets_pkey PRIMARY KEY (id);


--
-- Name: suppliers suppliers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.suppliers
    ADD CONSTRAINT suppliers_pkey PRIMARY KEY (id);


--
-- Name: user_pins user_pins_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_pins
    ADD CONSTRAINT user_pins_pkey PRIMARY KEY (id);


--
-- Name: user_pins user_pins_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_pins
    ADD CONSTRAINT user_pins_unique UNIQUE (user_id, pin_type, pin_key);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: users users_username_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_username_unique UNIQUE (username);


--
-- Name: warehouse_movements warehouse_movements_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.warehouse_movements
    ADD CONSTRAINT warehouse_movements_pkey PRIMARY KEY (id);


--
-- Name: warehouse_reservations warehouse_reservations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.warehouse_reservations
    ADD CONSTRAINT warehouse_reservations_pkey PRIMARY KEY (id);


--
-- Name: warehouses warehouses_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.warehouses
    ADD CONSTRAINT warehouses_name_key UNIQUE (name);


--
-- Name: warehouses warehouses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.warehouses
    ADD CONSTRAINT warehouses_pkey PRIMARY KEY (id);


--
-- Name: work_order_parts work_order_parts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_order_parts
    ADD CONSTRAINT work_order_parts_pkey PRIMARY KEY (id);


--
-- Name: work_order_revisions work_order_revisions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_order_revisions
    ADD CONSTRAINT work_order_revisions_pkey PRIMARY KEY (id);


--
-- Name: work_orders work_orders_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_orders
    ADD CONSTRAINT work_orders_code_key UNIQUE (code);


--
-- Name: work_orders work_orders_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_orders
    ADD CONSTRAINT work_orders_pkey PRIMARY KEY (id);


--
-- Name: workspace_members workspace_members_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workspace_members
    ADD CONSTRAINT workspace_members_pkey PRIMARY KEY (id);


--
-- Name: workspaces workspaces_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workspaces
    ADD CONSTRAINT workspaces_pkey PRIMARY KEY (id);


--
-- Name: idx_bom_document_parts_part; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bom_document_parts_part ON public.bom_document_parts USING btree (bom_part_id);


--
-- Name: idx_bom_documents_product; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bom_documents_product ON public.bom_documents USING btree (product_id);


--
-- Name: idx_bom_parts_parent_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bom_parts_parent_id ON public.bom_parts USING btree (parent_id);


--
-- Name: idx_bom_parts_product_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bom_parts_product_id ON public.bom_parts USING btree (product_id);


--
-- Name: idx_dni_note; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dni_note ON public.delivery_note_items USING btree (delivery_note_id);


--
-- Name: idx_orders_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orders_created_at ON public.orders USING btree (created_at DESC);


--
-- Name: idx_orders_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orders_status ON public.orders USING btree (status);


--
-- Name: idx_part_logs_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_part_logs_created_at ON public.part_logs USING btree (created_at DESC);


--
-- Name: idx_part_logs_part_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_part_logs_part_id ON public.part_logs USING btree (part_id);


--
-- Name: idx_part_logs_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_part_logs_user_id ON public.part_logs USING btree (user_id);


--
-- Name: idx_parts_department_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_parts_department_id ON public.parts USING btree (department_id);


--
-- Name: idx_parts_order_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_parts_order_id ON public.parts USING btree (order_id);


--
-- Name: idx_parts_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_parts_status ON public.parts USING btree (status);


--
-- Name: idx_po_quotes_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_po_quotes_order ON public.purchase_order_quotes USING btree (purchase_order_id);


--
-- Name: idx_project_bom_parts_bom_part; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_bom_parts_bom_part ON public.project_bom_parts USING btree (bom_part_id);


--
-- Name: idx_project_bom_parts_parent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_bom_parts_parent ON public.project_bom_parts USING btree (parent_custom_id);


--
-- Name: idx_project_bom_parts_pbom_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_bom_parts_pbom_id ON public.project_bom_parts USING btree (project_bom_id);


--
-- Name: idx_project_bom_product_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_bom_product_id ON public.project_bom USING btree (bom_product_id);


--
-- Name: idx_project_bom_project_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_bom_project_name ON public.project_bom USING btree (project_name);


--
-- Name: idx_purchase_items_po; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_purchase_items_po ON public.purchase_items USING btree (purchase_order_id);


--
-- Name: idx_purchase_items_project; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_purchase_items_project ON public.purchase_items USING btree (project_name);


--
-- Name: idx_purchase_items_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_purchase_items_status ON public.purchase_items USING btree (status);


--
-- Name: idx_purchase_items_warehouse; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_purchase_items_warehouse ON public.purchase_items USING btree (warehouse_id);


--
-- Name: idx_user_pins_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_pins_user_id ON public.user_pins USING btree (user_id);


--
-- Name: idx_users_is_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_is_active ON public.users USING btree (is_active);


--
-- Name: idx_users_username; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_username ON public.users USING btree (username);


--
-- Name: idx_warehouse_movements_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_warehouse_movements_created ON public.warehouse_movements USING btree (created_at DESC);


--
-- Name: idx_warehouse_movements_shipment_package; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_warehouse_movements_shipment_package ON public.warehouse_movements USING btree (shipment_package_id) WHERE (shipment_package_id IS NOT NULL);


--
-- Name: idx_warehouse_movements_warehouse; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_warehouse_movements_warehouse ON public.warehouse_movements USING btree (warehouse_id);


--
-- Name: idx_work_orders_department_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_work_orders_department_id ON public.work_orders USING btree (department_id);


--
-- Name: idx_work_orders_order_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_work_orders_order_id ON public.work_orders USING btree (order_id);


--
-- Name: idx_work_orders_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_work_orders_status ON public.work_orders USING btree (status);


--
-- Name: idx_work_orders_workspace_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_work_orders_workspace_id ON public.work_orders USING btree (workspace_id);


--
-- Name: idx_wres_project; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_wres_project ON public.warehouse_reservations USING btree (project_name);


--
-- Name: idx_wres_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_wres_status ON public.warehouse_reservations USING btree (status);


--
-- Name: order_documents_order_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX order_documents_order_idx ON public.order_documents USING btree (order_id);


--
-- Name: parts_order_code_ci_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX parts_order_code_ci_key ON public.parts USING btree (order_id, lower((code)::text));


--
-- Name: suppliers_name_lower_uq; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX suppliers_name_lower_uq ON public.suppliers USING btree (lower((name)::text));


--
-- Name: bom_document_parts bom_document_parts_doc_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bom_document_parts
    ADD CONSTRAINT bom_document_parts_doc_fk FOREIGN KEY (document_id) REFERENCES public.bom_documents(id) ON DELETE CASCADE;


--
-- Name: bom_document_parts bom_document_parts_part_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bom_document_parts
    ADD CONSTRAINT bom_document_parts_part_fk FOREIGN KEY (bom_part_id) REFERENCES public.bom_parts(id) ON DELETE CASCADE;


--
-- Name: bom_documents bom_documents_product_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bom_documents
    ADD CONSTRAINT bom_documents_product_fk FOREIGN KEY (product_id) REFERENCES public.bom_products(id) ON DELETE CASCADE;


--
-- Name: bom_parts bom_parts_parent_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bom_parts
    ADD CONSTRAINT bom_parts_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES public.bom_parts(id) ON DELETE CASCADE;


--
-- Name: bom_parts bom_parts_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bom_parts
    ADD CONSTRAINT bom_parts_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.bom_products(id) ON DELETE CASCADE;


--
-- Name: delivery_note_items delivery_note_items_delivery_note_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.delivery_note_items
    ADD CONSTRAINT delivery_note_items_delivery_note_id_fkey FOREIGN KEY (delivery_note_id) REFERENCES public.delivery_notes(id) ON DELETE CASCADE;


--
-- Name: delivery_note_items delivery_note_items_warehouse_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.delivery_note_items
    ADD CONSTRAINT delivery_note_items_warehouse_id_fkey FOREIGN KEY (warehouse_id) REFERENCES public.warehouses(id) ON DELETE SET NULL;


--
-- Name: delivery_notes delivery_notes_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.delivery_notes
    ADD CONSTRAINT delivery_notes_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE SET NULL;


--
-- Name: departments departments_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.departments
    ADD CONSTRAINT departments_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE CASCADE;


--
-- Name: order_documents order_documents_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_documents
    ADD CONSTRAINT order_documents_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE CASCADE;


--
-- Name: order_items order_items_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT order_items_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE CASCADE;


--
-- Name: orders orders_approved_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_approved_by_fkey FOREIGN KEY (approved_by) REFERENCES public.users(id);


--
-- Name: part_logs part_logs_part_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.part_logs
    ADD CONSTRAINT part_logs_part_id_fkey FOREIGN KEY (part_id) REFERENCES public.parts(id) ON DELETE CASCADE;


--
-- Name: part_logs part_logs_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.part_logs
    ADD CONSTRAINT part_logs_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: parts parts_department_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.parts
    ADD CONSTRAINT parts_department_id_fkey FOREIGN KEY (department_id) REFERENCES public.departments(id) ON DELETE SET NULL;


--
-- Name: parts parts_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.parts
    ADD CONSTRAINT parts_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE CASCADE;


--
-- Name: parts parts_parent_part_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.parts
    ADD CONSTRAINT parts_parent_part_fk FOREIGN KEY (parent_part_id) REFERENCES public.parts(id) ON DELETE SET NULL;


--
-- Name: project_bom project_bom_bom_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_bom
    ADD CONSTRAINT project_bom_bom_product_id_fkey FOREIGN KEY (bom_product_id) REFERENCES public.bom_products(id) ON DELETE CASCADE;


--
-- Name: project_bom_parts project_bom_parts_bom_part_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_bom_parts
    ADD CONSTRAINT project_bom_parts_bom_part_id_fkey FOREIGN KEY (bom_part_id) REFERENCES public.bom_parts(id) ON DELETE SET NULL;


--
-- Name: project_bom_parts project_bom_parts_dept_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_bom_parts
    ADD CONSTRAINT project_bom_parts_dept_id_fkey FOREIGN KEY (dept_id) REFERENCES public.departments(id) ON DELETE SET NULL;


--
-- Name: project_bom_parts project_bom_parts_parent_custom_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_bom_parts
    ADD CONSTRAINT project_bom_parts_parent_custom_id_fkey FOREIGN KEY (parent_custom_id) REFERENCES public.project_bom_parts(id) ON DELETE CASCADE;


--
-- Name: project_bom_parts project_bom_parts_project_bom_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_bom_parts
    ADD CONSTRAINT project_bom_parts_project_bom_id_fkey FOREIGN KEY (project_bom_id) REFERENCES public.project_bom(id) ON DELETE CASCADE;


--
-- Name: project_date_revisions project_date_revisions_project_date_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_date_revisions
    ADD CONSTRAINT project_date_revisions_project_date_id_fkey FOREIGN KEY (project_date_id) REFERENCES public.project_dates(id) ON DELETE CASCADE;


--
-- Name: project_date_revisions project_date_revisions_revised_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_date_revisions
    ADD CONSTRAINT project_date_revisions_revised_by_fkey FOREIGN KEY (revised_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: project_dates project_dates_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_dates
    ADD CONSTRAINT project_dates_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE CASCADE;


--
-- Name: purchase_items purchase_items_pbp_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.purchase_items
    ADD CONSTRAINT purchase_items_pbp_fkey FOREIGN KEY (project_bom_part_id) REFERENCES public.project_bom_parts(id) ON DELETE SET NULL;


--
-- Name: purchase_items purchase_items_po_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.purchase_items
    ADD CONSTRAINT purchase_items_po_fkey FOREIGN KEY (purchase_order_id) REFERENCES public.purchase_orders(id) ON DELETE SET NULL;


--
-- Name: purchase_items purchase_items_stock_plan_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.purchase_items
    ADD CONSTRAINT purchase_items_stock_plan_fk FOREIGN KEY (stock_plan_id) REFERENCES public.purchase_items(id) ON DELETE SET NULL;


--
-- Name: purchase_items purchase_items_warehouse_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.purchase_items
    ADD CONSTRAINT purchase_items_warehouse_fkey FOREIGN KEY (warehouse_id) REFERENCES public.warehouses(id) ON DELETE SET NULL;


--
-- Name: purchase_order_quotes purchase_order_quotes_po_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.purchase_order_quotes
    ADD CONSTRAINT purchase_order_quotes_po_fkey FOREIGN KEY (purchase_order_id) REFERENCES public.purchase_orders(id) ON DELETE CASCADE;


--
-- Name: purchase_orders purchase_orders_selected_quote_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.purchase_orders
    ADD CONSTRAINT purchase_orders_selected_quote_fkey FOREIGN KEY (selected_quote_id) REFERENCES public.purchase_order_quotes(id) ON DELETE SET NULL;


--
-- Name: shipment_package_items shipment_package_items_package_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shipment_package_items
    ADD CONSTRAINT shipment_package_items_package_id_fkey FOREIGN KEY (package_id) REFERENCES public.shipment_packages(id) ON DELETE CASCADE;


--
-- Name: shipment_package_items shipment_package_items_part_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shipment_package_items
    ADD CONSTRAINT shipment_package_items_part_id_fkey FOREIGN KEY (part_id) REFERENCES public.parts(id) ON DELETE SET NULL;


--
-- Name: shipment_package_items shipment_package_items_project_bom_part_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shipment_package_items
    ADD CONSTRAINT shipment_package_items_project_bom_part_id_fkey FOREIGN KEY (project_bom_part_id) REFERENCES public.project_bom_parts(id) ON DELETE SET NULL;


--
-- Name: shipment_packages shipment_packages_delivery_note_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shipment_packages
    ADD CONSTRAINT shipment_packages_delivery_note_id_fkey FOREIGN KEY (delivery_note_id) REFERENCES public.delivery_notes(id) ON DELETE SET NULL;


--
-- Name: shipment_packages shipment_packages_warehouse_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shipment_packages
    ADD CONSTRAINT shipment_packages_warehouse_id_fkey FOREIGN KEY (warehouse_id) REFERENCES public.warehouses(id) ON DELETE SET NULL;


--
-- Name: user_pins user_pins_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_pins
    ADD CONSTRAINT user_pins_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: warehouse_movements warehouse_movements_delivery_note_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.warehouse_movements
    ADD CONSTRAINT warehouse_movements_delivery_note_id_fkey FOREIGN KEY (delivery_note_id) REFERENCES public.delivery_notes(id) ON DELETE SET NULL;


--
-- Name: warehouse_movements warehouse_movements_purchase_item_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.warehouse_movements
    ADD CONSTRAINT warehouse_movements_purchase_item_fkey FOREIGN KEY (purchase_item_id) REFERENCES public.purchase_items(id) ON DELETE SET NULL;


--
-- Name: warehouse_movements warehouse_movements_reservation_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.warehouse_movements
    ADD CONSTRAINT warehouse_movements_reservation_id_fkey FOREIGN KEY (reservation_id) REFERENCES public.warehouse_reservations(id) ON DELETE SET NULL;


--
-- Name: warehouse_movements warehouse_movements_shipment_package_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.warehouse_movements
    ADD CONSTRAINT warehouse_movements_shipment_package_id_fkey FOREIGN KEY (shipment_package_id) REFERENCES public.shipment_packages(id) ON DELETE CASCADE;


--
-- Name: warehouse_movements warehouse_movements_warehouse_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.warehouse_movements
    ADD CONSTRAINT warehouse_movements_warehouse_fkey FOREIGN KEY (warehouse_id) REFERENCES public.warehouses(id) ON DELETE RESTRICT;


--
-- Name: warehouse_reservations warehouse_reservations_target_warehouse_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.warehouse_reservations
    ADD CONSTRAINT warehouse_reservations_target_warehouse_id_fkey FOREIGN KEY (target_warehouse_id) REFERENCES public.warehouses(id) ON DELETE SET NULL;


--
-- Name: warehouse_reservations warehouse_reservations_warehouse_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.warehouse_reservations
    ADD CONSTRAINT warehouse_reservations_warehouse_id_fkey FOREIGN KEY (warehouse_id) REFERENCES public.warehouses(id) ON DELETE RESTRICT;


--
-- Name: warehouses warehouses_responsible_user_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.warehouses
    ADD CONSTRAINT warehouses_responsible_user_fkey FOREIGN KEY (responsible_user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: work_order_parts work_order_parts_part_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_order_parts
    ADD CONSTRAINT work_order_parts_part_id_fkey FOREIGN KEY (part_id) REFERENCES public.parts(id) ON DELETE CASCADE;


--
-- Name: work_order_parts work_order_parts_work_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_order_parts
    ADD CONSTRAINT work_order_parts_work_order_id_fkey FOREIGN KEY (work_order_id) REFERENCES public.work_orders(id) ON DELETE CASCADE;


--
-- Name: work_order_revisions work_order_revisions_revised_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_order_revisions
    ADD CONSTRAINT work_order_revisions_revised_by_fkey FOREIGN KEY (revised_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: work_order_revisions work_order_revisions_work_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_order_revisions
    ADD CONSTRAINT work_order_revisions_work_order_id_fkey FOREIGN KEY (work_order_id) REFERENCES public.work_orders(id) ON DELETE CASCADE;


--
-- Name: work_orders work_orders_assigned_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_orders
    ADD CONSTRAINT work_orders_assigned_user_id_fkey FOREIGN KEY (assigned_user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: work_orders work_orders_department_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_orders
    ADD CONSTRAINT work_orders_department_id_fkey FOREIGN KEY (department_id) REFERENCES public.departments(id) ON DELETE SET NULL;


--
-- Name: work_orders work_orders_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_orders
    ADD CONSTRAINT work_orders_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE CASCADE;


--
-- Name: work_orders work_orders_workspace_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_orders
    ADD CONSTRAINT work_orders_workspace_id_fkey FOREIGN KEY (workspace_id) REFERENCES public.workspaces(id) ON DELETE SET NULL;


--
-- Name: workspace_members workspace_members_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workspace_members
    ADD CONSTRAINT workspace_members_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: workspace_members workspace_members_workspace_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workspace_members
    ADD CONSTRAINT workspace_members_workspace_id_fkey FOREIGN KEY (workspace_id) REFERENCES public.workspaces(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict decTJE1mEtHFzncHa51wE2CfaB2rgLghmsP3L86tHen6ayR2aUog9T1fLR7XVe8

