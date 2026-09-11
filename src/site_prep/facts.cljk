(ns site-prep.facts
  "Per-jurisdiction site-preparation-safety regulatory catalog -- the
  spec-basis table the Site Prep Governor checks every `:schedule-site-
  operation` proposal against ('did the advisor cite an OFFICIAL public
  source for this jurisdiction's utility-locate/excavation-notification
  requirements, or did it invent one?'). Same honest-coverage discipline
  `construction.facts`/`demolition.facts` (`cloud-itonami-isic-4211`/
  `cloud-itonami-isic-4311`) established for this fleet: a jurisdiction
  not in this table has NO spec-basis, full stop -- the advisor must not
  fabricate one, and the governor holds if it tries.

  Coverage is reported HONESTLY (see `coverage`); this is a STARTING
  catalog (JPN/USA/DEU), not a from-scratch survey of all ~194
  jurisdictions. Extending coverage is additive: add one map to `catalog`,
  cite a real source, done -- never invent a jurisdiction's requirements
  to make coverage look bigger.

  `:threshold-model` mirrors the SAME honest quantitative/qualitative
  split `construction.facts/threshold-model` and `demolition.facts/
  threshold-model` established, applied here to a DIFFERENT real-world
  numeric trigger -- the minimum number of days an excavation-work
  notification filing must precede the planned start of work:
    :quantitative -- the law itself states a fixed lead-time (Japan's 7
                     days under the Noise Regulation Act's specified-
                     construction-work notice; the USA's widely-adopted
                     2-business-day 811 one-call minimum).
                     `notification-lead-insufficient?` can independently
                     recompute a HARD hold from this.
    :qualitative  -- the jurisdiction imposes a documented duty to query
                     utility/network operators before excavation, with NO
                     fixed EU-wide numeric lead-time (Germany/EU). This
                     actor does NOT invent a day-count to make this
                     jurisdiction look automatable -- `notification-lead-
                     insufficient?` returns `:qualitative` and the Site
                     Prep Governor's permanent high-stakes gate on
                     `:schedule-site-operation` (see `site-prep.governor`
                     ns docstring) routes the decision to a human every
                     time regardless.

  DEU is used as the EU-jurisdiction proxy, the SAME convention
  `construction.facts`/`demolition.facts`/`aerospace.facts` established --
  there is no ISO-3166 alpha-3 code for the EU itself and excavation/
  earthworks safety law is largely national/state (Land) level in the EU,
  so the citation lists the German national standard rather than
  inventing an EU country code.

  IMPORTANT distinction from `demolition.facts`: this actor's
  quantitative-notification citation for the USA is honestly labeled a
  WIDELY-ADOPTED STATE-LAW CONVENTION coordinated through the national 811
  one-call number, NOT a single federal statute with one fixed day count
  (unlike demolition's federal NESHAP 10-working-day rule) -- the same
  'model code, not federal statute' honesty labeling
  `construction.facts`/`demolition.facts` use for the IBC permit
  citation."
  )

(def catalog
  "iso3 -> requirement map. `:utility-locate-basis` / `:excavation-
  notification-basis` / their `-provenance` pairs, plus `:owner-
  authority`, are the G2-style citation the governor requires before a
  `:schedule-site-operation` proposal can ever commit."
  {"JPN" {:name "Japan"
          :owner-authority "厚生労働省（労働基準監督署）／市町村長（騒音規制法の特定建設作業届出先）"
          :utility-locate-basis "労働安全衛生規則（昭和47年労働省令第32号）第355条（地山の掘削の作業を行う場合において、地山の崩壊・埋設物等の損壊により労働者に危険を及ぼすおそれがあるときは、あらかじめ作業箇所及びその周辺の地山をボーリングその他適当な方法により調査する義務）"
          :utility-locate-provenance "https://laws.e-gov.go.jp/law/347M50002000032"
          :excavation-notification-basis "騒音規制法第14条（規制地域内で特定建設作業を伴う建設工事を施工しようとする者は、当該特定建設作業の開始の日の7日前までに市町村長に届け出る義務。バックホウ〈定格出力80kW以上〉・ブルドーザー〈定格出力40kW以上〉等の掘削・整地機械を使用する作業が対象）"
          :excavation-notification-provenance "https://laws.e-gov.go.jp/law/343AC0000000098"
          :threshold-model :quantitative
          :notification-lead-days 7
          :threshold-note "特定建設作業（一定出力以上のバックホウ・ブルドーザー等を使用する掘削・整地作業を含む）開始の7日前までの市町村長への届出義務（騒音規制法第14条）。"}
   "USA" {:name "United States"
          :owner-authority "State one-call center (coordinated nationally via the 811 number, FCC-designated 2005) / Occupational Safety and Health Administration (OSHA), U.S. Department of Labor"
          :utility-locate-basis "OSHA 29 CFR 1926.651(b) (Specific Excavation Requirements -- Underground installations: the estimated location of utility installations reasonably expected to be encountered during excavation must be determined before opening an excavation, and utility owners must be contacted and asked to establish the location of underground installations before excavation begins)"
          :utility-locate-provenance "https://www.osha.gov/laws-regs/regulations/standardnumber/1926/1926.651"
          :excavation-notification-basis "State one-call ('Call Before You Dig') statutes, coordinated nationally via the 811 abbreviated dialing code (Pipeline Safety Improvement Act of 2002 / FCC 811 designation, 2005) -- most states require notifying 811 at least 2 business days before excavation begins (e.g. Texas, New York)"
          :excavation-notification-provenance "https://811beforeyoudig.com/"
          :threshold-model :quantitative
          :notification-lead-days 2
          :threshold-note "811は連邦が調整する全国共通番号だが、実際の届出リードタイムは州法（one-call statute）ごとに規定される。テキサス州・ニューヨーク州等、多くの州で採用されている最短基準である『2営業日前』を採用 -- 単一の連邦成文法上の固定日数ではないことを明示（construction.facts/demolition.factsのIBC honesty labelingと同じ扱い）。"
          :notification-note "State-law convention (via national 811 coordination), not a single federal statute -- the same honest labeling `construction.facts`/`demolition.facts` use for their IBC/model-code citations."}
   "DEU" {:name "Germany (EU jurisdiction proxy, see ns docstring)"
          :owner-authority "Landesbehörden（州の建設・労働当局）／ Deutsches Institut für Normung (DIN)"
          :utility-locate-basis "EU/ドイツには埋設物照会に関する統一の成文法上の固定日数リードタイムは存在しない。ドイツの実務では、掘削作業前に埋設物管理者への照会（Leitungsauskunft）を行うことが一般的注意義務（Verkehrssicherungspflicht、民法(BGB)第823条及び州建築法（Landesbauordnung）に由来）として要求される。掘削・溝掘り作業自体の法面・土留め設計基準は DIN 4124:2012-01（Baugruben und Gräben -- Böschungen, Verbau, Arbeitsraumbreiten）が定める。"
          :utility-locate-provenance "https://www.dinmedia.de/en/standard/din-4124/147362129"
          :threshold-model :qualitative
          :notification-lead-days nil
          :threshold-note "ドイツ/EUの掘削作業に関する法制は、事前の埋設物照会（Leitungsauskunft）及び一般的注意義務（Verkehrssicherungspflicht）を課すのみで、日本の7日・米国の2営業日のような固定日数の届出リードタイムはEU全域では法定されていない -- ここで数値を創作しない。"
          :excavation-notification-basis "Landesbauordnung（州建築法、標準法として Musterbauordnung MBO 参照）に基づく着工届（Baubeginnanzeige）-- 固定日数のリードタイムは州法ごとに異なり、EU全域の統一値は法定されていない。"
          :excavation-notification-provenance "https://www.dibt.de/en"
          :excavation-notification-note "German site-preparation notification requirements are governed by the state Landesbauordnungen, not federal law -- the same honest state-vs-federal layering `construction.facts`/`demolition.facts` use for their own DEU citations."}})

(defn spec-basis
  "The jurisdiction's requirement map, or nil -- nil means NO spec-basis,
  and the governor must hold any `:schedule-site-operation` proposal that
  tries to cite one."
  [iso3]
  (get catalog iso3))

(defn coverage
  "Honest coverage report: how many of the requested jurisdictions actually
  have a spec-basis entry. Never report a missing jurisdiction as covered."
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-isic-4312 R0: " (count catalog)
                 " jurisdictions seeded with an official spec-basis. "
                 "This is a starting catalog, not a survey of all ~194 "
                 "jurisdictions -- extend `site-prep.facts/catalog`, "
                 "never fabricate a jurisdiction's requirements.")})))

(defn notification-lead-insufficient?
  "Independently recompute whether `site`'s own recorded
  `:notification-lead-days-actual` (the site's own permanent recorded
  field -- days between the excavation-work notification filing and the
  planned start of work) falls SHORT of `iso3`'s regulatory minimum lead
  time.

  Three-valued, deliberately (the same shape `construction.facts/weather-
  threshold-exceeded?` and `demolition.facts/notification-lead-
  insufficient?` established):
    true         -- a :quantitative jurisdiction (Japan, USA) whose own
                    numeric minimum lead time is independently confirmed
                    NOT met by the site's own recorded actual -- a
                    bright-line legal violation. The Site Prep Governor
                    turns this into a HARD, un-overridable hold on
                    `:schedule-site-operation`.
    false        -- a :quantitative jurisdiction confirmed sufficient.
    :qualitative -- a jurisdiction with NO fixed numeric lead-time (DEU/
                    EU). This actor cannot independently confirm
                    'sufficient' or 'insufficient' by arithmetic alone --
                    the law itself requires a documented judgment call.
                    Never fabricate a lead-time here. The Site Prep
                    Governor relies on its permanent high-stakes gate for
                    `:schedule-site-operation` (ALWAYS escalates to a
                    human, at every phase) rather than a HARD numeric rule
                    in this case.
    nil          -- no spec-basis at all for `iso3` (a jurisdiction not in
                    `catalog`)."
  [iso3 {:keys [notification-lead-days-actual]}]
  (when-let [{:keys [threshold-model notification-lead-days]} (spec-basis iso3)]
    (case threshold-model
      :quantitative
      (boolean (and (number? notification-lead-days-actual)
                    (< notification-lead-days-actual notification-lead-days)))
      :qualitative
      :qualitative
      nil)))
