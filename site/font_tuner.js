/** FONT TUNER (dev only, enable with --font-tuner true) **/

(function () {
  const storageKey = 'font-tuner';
  const categories = {'Sans Serif': 'sans-serif', 'Serif': 'serif', 'Monospace': 'monospace', 'Handwriting': 'cursive'};
  const cloud = ' ☁️'; // marks Google Fonts in the list

  const fieldDefs = {
    weight:     {label: 'weight',   step: 10,    unit: '',   min: 1},
    size:       {label: 'size',     step: 1,     unit: 'px', min: 1},
    wdth:       {label: 'wdth',     step: 1,     unit: '',   min: 1, auto: true},
    opsz:       {label: 'opsz',     step: 1,     unit: '',   min: 1, auto: true},
    lineHeight: {label: 'line',     step: 1,     unit: 'px', min: 1},
    tracking:   {label: 'tracking', step: 0.001, unit: 'em', digits: 3},
    spacing:    {label: 'spacing',  step: 1,     unit: 'px', min: 0},
    above:      {label: 'above',    step: 1,     unit: 'px', min: 0},
    width:      {label: 'width',    step: 10,    unit: 'px', min: 1},
    chars:      {label: '≈',        step: 1,     unit: 'ch', min: 1},
    align:      {options: ['left', 'center']},
    margin:     {label: 'margin',   step: 1,     unit: 'px', min: 0},
  };

  // Grid columns, so the same field lines up across rows ('above' shares a column with 'spacing')
  const columns = ['font', 'weight', 'size', 'wdth', 'opsz', 'lineHeight', 'tracking', 'spacing', 'width', 'chars', 'align', 'margin'];
  const columnOf = (key) => key === 'above' ? 'spacing' : key;

  const groups = {
    head: {
      label:    'Headers',
      selector: '.title, h1, h2, h3, .loud',
      fields:   ['font', 'weight', 'size', 'wdth', 'opsz', 'lineHeight', 'tracking', 'above'],
    },
    body: {
      label:    'Body',
      selector: 'body',
      fields:   ['font', 'weight', 'size', 'wdth', 'opsz', 'lineHeight', 'tracking', 'spacing', 'width', 'chars', 'align', 'margin'],
    },
    small: {
      label:    'Small',
      selector: 'figcaption, .foot, .note, .date, .footer, .about_inner, .page_about dl, .projects dl',
      fields:   ['font', 'weight', 'size', 'wdth', 'opsz', 'lineHeight', 'tracking'],
    },
    code: {
      label:    'Code',
      selector: 'pre, code',
      fields:   ['font', 'weight', 'size', 'wdth', 'opsz', 'lineHeight', 'tracking'],
    },
  };
  const groupOrder = ['head', 'body', 'small', 'code'];

  // Body keys are unprefixed (stored before there were groups), others are 'head.size' etc
  const fk = (g, key) => g === 'body' ? key : `${g}.${key}`;

  let overrides = JSON.parse(localStorage.getItem(storageKey) || '{}');
  if ('gap' in overrides) {
    // 'gap' used to override --gap everywhere, now it's paragraph spacing only
    overrides.spacing = overrides.gap;
    delete overrides.gap;
  }
  let defaults = {};
  let googleFamilies = new Map(); // lowercase name -> metadata
  let localFamilies = new Map();  // lowercase name -> metadata
  // CSS generic families, listed first. wdth is SF Pro's range (system-ui on macOS)
  const systemFamilies = new Map([['system-ui', {family: 'system-ui', category: 'Sans Serif', wdth: [30, 150]}]]);
  let fontNames = [...systemFamilies.keys()]; // everything in the dropdown: system first, then sorted
  let loaded = new Set();
  let preview = null;             // {g, name}: font highlighted in a dropdown but not chosen yet
  let inputs = {head: {}, body: {}, small: {}, code: {}};
  let combos = {};
  let alignButtons = {};
  let styleEl, measureEl, root;

  /** UTILS **/

  function save() {
    localStorage.setItem(storageKey, JSON.stringify(overrides));
  }

  function fmt(n, digits = 2) {
    return String(Math.round(n * 10 ** digits) / 10 ** digits);
  }

  function el(tag, {dataset, ...props} = {}, ...children) {
    const e = Object.assign(document.createElement(tag), props);
    Object.assign(e.dataset, dataset);
    e.append(...children);
    return e;
  }

  function measure(css) {
    const e = el('div');
    e.style.cssText = 'position: absolute; visibility: hidden; ' + css;
    document.body.appendChild(e);
    const rect = e.getBoundingClientRect();
    e.remove();
    return rect;
  }

  // Computed text style of a hidden element, e.g. probe('h1', 'title')
  function probe(tag, className = '') {
    const e = el(tag, {className, textContent: 'Probe'});
    e.style.cssText = 'position: absolute; visibility: hidden;';
    document.body.appendChild(e);
    const cs = getComputedStyle(e);
    const size = parseFloat(cs.fontSize);
    const axis = (tag) => parseFloat(cs.fontVariationSettings.match(new RegExp(`"${tag}" ([\\d.]+)`))?.[1]) || undefined;
    const res = {
      font:       cs.fontFamily.split(',')[0].trim().replace(/^["']|["']$/g, ''),
      family:     cs.fontFamily,
      weight:     parseFloat(cs.fontWeight),
      size:       size,
      lineRatio:  (cs.lineHeight === 'normal' ? size * 1.2 : parseFloat(cs.lineHeight)) / size,
      tracking:   cs.letterSpacing === 'normal' ? 0 : parseFloat(cs.letterSpacing) / size,
      wdth:       axis('wdth'),
      opsz:       axis('opsz'),
      above:      parseFloat(cs.marginTop),
    };
    e.remove();
    return res;
  }

  // Code block: size of `pre > code`, line height = how much a second line adds to the block
  function probeCodeBlock() {
    const code = el('code', {textContent: 'Probe'});
    const pre = el('pre', {}, code);
    pre.style.cssText = 'position: absolute; visibility: hidden;';
    (document.querySelector('.content') || document.body).appendChild(pre);
    const size = parseFloat(getComputedStyle(code).fontSize);
    const oneLine = pre.getBoundingClientRect().height;
    code.textContent = 'Probe\nProbe';
    const lineRatio = (pre.getBoundingClientRect().height - oneLine) / size;
    pre.remove();
    return {size, lineRatio};
  }

  function readDefaults() {
    const body  = probe('div');
    const title = probe('h1', 'title');
    const note  = probe('p', 'note');
    const h2    = probe('h2');
    const code  = probe('code');
    const block = probeCodeBlock();
    defaults = {
      body: {
        font:      body.font,
        fallback:  body.family.split(',').slice(1).join(',').trim() || 'sans-serif',
        weight:    body.weight,
        size:      body.size,
        lineRatio: body.lineRatio,
        tracking:  body.tracking,
        wdth:      body.wdth,
        opsz:      body.opsz,
        spacing:   measure('height: var(--spacing)').height,
        width:     measure('width: var(--width)').width,
        align:     'left',
        margin:    measure('width: var(--margin)').width,
      },
      head: {
        font:      title.font,
        weight:    title.weight,
        size:      title.size,
        lineRatio: title.lineRatio,
        tracking:  title.tracking,
        wdth:      title.wdth,
        opsz:      title.opsz,
        above:     h2.above,
        sizes:     {title: title.size, h1: probe('h1').size, h2: probe('h2').size, h3: probe('h3').size},
      },
      small: {
        font:      note.font,
        weight:    note.weight,
        size:      note.size,
        lineRatio: note.lineRatio,
        tracking:  note.tracking,
        wdth:      note.wdth,
        opsz:      note.opsz,
      },
      code: {
        font:      code.font,
        fallback:  code.family.split(',').slice(1).join(',').trim() || 'monospace',
        weight:    code.weight,
        size:      block.size,
        inlineRatio: code.size / block.size,
        lineRatio: block.lineRatio,
        tracking:  code.tracking,
        wdth:      code.wdth,
        opsz:      code.opsz,
      },
    };
  }

  // Effective value: override, or what the page would use without it
  function value(g, key) {
    const k = fk(g, key);
    if (k in overrides) {
      return overrides[k];
    }
    if (key === 'lineHeight') {
      return value(g, 'size') * defaults[g].lineRatio;
    }
    return defaults[g][key];
  }

  /** CHARS PER LINE **/

  function sampleText() {
    let text = '';
    for (const p of document.querySelectorAll('.content p')) {
      text += p.textContent + ' ';
      if (text.length > 3000) break;
    }
    text = text.replace(/\s+/g, ' ').trim();
    return text.length > 200 ? text : 'The quick brown fox jumps over the lazy dog. Pack my box with five dozen liquor jugs. How vexingly quick daft zebras jump!';
  }

  function avgCharWidth() {
    if (!measureEl) {
      measureEl = el('span', {textContent: sampleText()});
      measureEl.style.cssText = 'position: absolute; visibility: hidden; white-space: nowrap; left: 0; top: 0; width: max-content; max-width: none;';
      (document.querySelector('.content') || document.body).appendChild(measureEl);
    }
    return measureEl.getBoundingClientRect().width / measureEl.textContent.length;
  }

  /** FONTS **/

  function familyCss(f) {
    const name = encodeURIComponent(f.family);
    if (f.wght || f.wdth || f.opsz) {
      // Axes must be listed alphabetically: ital, opsz, wdth, wght
      const axes = [['opsz', f.opsz], ['wdth', f.wdth], ['wght', f.wght]].filter(([_, r]) => r);
      const tags = axes.map(([t]) => t).join(',');
      const ranges = axes.map(([_, r]) => r[0] + '..' + r[1]).join(',');
      return f.italics?.length
        ? `${name}:ital,${tags}@0,${ranges};1,${ranges}`
        : `${name}:${tags}@${ranges}`;
    }
    const tuples = (f.weights || []).map(w => '0,' + w).concat((f.italics || []).map(w => '1,' + w));
    return tuples.length ? `${name}:ital,wght@${tuples.join(';')}` : name;
  }

  function loadStylesheet(href) {
    if (loaded.has(href)) return;
    loaded.add(href);
    document.head.appendChild(el('link', {rel: 'stylesheet', href}));
  }

  // 'Inter ☁️' is Google, plain 'Inter' is local if installed, Google otherwise
  function resolve(name) {
    const key = name.trim().toLowerCase();
    const system = systemFamilies.get(key);
    if (system) return {system: true, f: system, name: system.family};
    if (key.endsWith(cloud)) {
      const f = googleFamilies.get(key.slice(0, -cloud.length));
      return f && {google: true, f, name: f.family + cloud};
    }
    const local = localFamilies.get(key);
    if (local) return {google: false, f: local, name: local.family};
    const f = googleFamilies.get(key);
    return f && {google: true, f, name: f.family + cloud};
  }

  // Font set on the group itself (previewed or chosen), not inherited
  function ownFont(g) {
    return preview?.g === g ? preview.name : overrides[fk(g, 'font')];
  }

  function effectiveFont(g) {
    return ownFont(g) ?? defaults[g].font;
  }

  // [min, max] of the group's font variable axis ('wdth', 'opsz'), if it has one
  function axisRange(g, axis) {
    const font = effectiveFont(g);
    return font ? resolve(font)?.f[axis] : undefined;
  }

  // Local fonts get a separate CSS family so they don't mix with Google or site fonts of the same name
  function fontStack(g, name, forCopy) {
    const r = resolve(name);
    const groupFallback = defaults[g].fallback || defaults.body.fallback;
    if (!r) {
      return `'${name}', ${groupFallback}`;
    }
    const fallback = categories[r.f.category] || groupFallback;
    if (r.system) {
      return `${r.f.family}, ${fallback}`; // unquoted, or it's a family name instead of a keyword
    }
    if (forCopy) {
      return `'${r.f.family}', ${fallback}`;
    }
    if (r.google) {
      loadStylesheet(`https://fonts.googleapis.com/css2?family=${familyCss(r.f)}&display=swap`);
      return `'${r.f.family}', ${fallback}`;
    }
    loadStylesheet('/font-tuner/local.css?family=' + encodeURIComponent(r.f.family));
    return `'${r.f.family} (local)', ${fallback}`;
  }

  async function fetchFamilies() {
    try {
      const {google, local} = await (await fetch('/font-tuner/fonts.json')).json();
      const names = [];
      for (const f of google) {
        googleFamilies.set(f.family.toLowerCase(), f);
        names.push(f.family + cloud);
      }
      for (const f of local) {
        localFamilies.set(f.family.toLowerCase(), f);
        names.push(f.family);
      }
      fontNames = [...systemFamilies.keys(), ...names.sort((a, b) => a.localeCompare(b, 'en', {sensitivity: 'base'}))];
      apply();
      refresh();
    } catch (e) {
      console.warn('Font tuner: failed to fetch font list', e);
    }
  }

  /** APPLYING **/

  function centerCss() {
    return `.page, .content, .cover, .content > figure { align-items: center; }
.page { padding-left: 0; }
.fig, .loud, .about { margin-left: 0; }
.foot, figcaption, .loud { text-align: center; }
`;
  }

  function css(forCopy) {
    const rules = [];
    const rule = (selector, decls) => decls.length && rules.push(`${selector} { ${decls.join('; ')}; }`);
    const has = (g, key) => fk(g, key) in overrides;
    const get = (g, key) => overrides[fk(g, key)];

    const vars = [];
    if (has('body', 'size')) {
      vars.push(`--font-size: ${get('body', 'size')}px`);
    }
    if (has('body', 'width')) vars.push(`--width: ${get('body', 'width')}px`);
    if (has('body', 'margin')) vars.push(`--margin: ${get('body', 'margin')}px`);
    rule(':root', vars);

    // wdth is inherited from body like the font itself, opsz is specific to the group's size
    const variations = (g) => [
      value(g, 'wdth') !== undefined && axisRange(g, 'wdth') && `"wdth" ${value(g, 'wdth')}`,
      value(g, 'opsz') !== undefined && axisRange(g, 'opsz') && `"opsz" ${value(g, 'opsz')}`,
    ].filter(Boolean);
    const bodyVariations = variations('body');
    for (const g of ['body', 'head', 'small', 'code']) {
      const decls = [];
      const font = ownFont(g);
      if (font) decls.push(`font-family: ${fontStack(g, font, forCopy)}`);
      if (has(g, 'weight')) decls.push(`font-weight: ${get(g, 'weight')}`);
      if (g === 'small' && has(g, 'size')) decls.push(`font-size: ${get(g, 'size')}px`);
      if (g !== 'code' && has(g, 'lineHeight')) decls.push(`line-height: ${Math.round(get(g, 'lineHeight') / value(g, 'size') * 1000) / 1000} /* ${get(g, 'lineHeight')}px */`);
      // letter-spacing in em is inherited as px, so restate it for headers/small to keep it relative
      if (has(g, 'tracking') || (g !== 'body' && has('body', 'tracking'))) decls.push(`letter-spacing: ${value(g, 'tracking')}em`);
      const vs = variations(g);
      if (vs.length) {
        decls.push(`font-variation-settings: ${vs.join(', ')}`);
      } else if (g !== 'body' && bodyVariations.length) {
        decls.push('font-variation-settings: normal'); // don't inherit body's opsz
      }
      rule(groups[g].selector, decls);
    }

    if (has('head', 'size')) {
      // Header size is the post title, other headers scale proportionally
      const {sizes} = defaults.head;
      const s = get('head', 'size');
      for (const h of ['h1', 'h2', 'h3']) rule(h, [`font-size: ${fmt(sizes[h] * s / sizes.title)}px`]);
      rule('.loud', [`font-size: ${fmt(sizes.h1 * s / sizes.title)}px`]);
      rule('.title', [`font-size: ${s}px`]);
    }
    if (has('code', 'size')) {
      // Code size is the code block, inline code scales proportionally
      rule('code', [`font-size: ${fmt(get('code', 'size') * defaults.code.inlineRatio)}px`]);
    }
    if (has('code', 'size') || has('code', 'lineHeight')) {
      // Same size on `pre` and `pre > code`, so pre's own line height doesn't stretch the lines
      rule('pre, pre > code', [`font-size: ${fmt(value('code', 'size'))}px`, `line-height: ${fmt(value('code', 'lineHeight'))}px`]);
    }
    if (has('head', 'above')) rule('h1, h2, h3', [`margin-top: ${get('head', 'above')}px`]);
    if (has('body', 'spacing')) rule('.content, .content ul, .content ol, blockquote', [`gap: ${get('body', 'spacing')}px`]);

    return rules.map(r => r + '\n').join('')
         + (value('body', 'align') === 'center' ? centerCss() : '');
  }

  function apply() {
    styleEl.textContent = css();
  }

  function refresh() {
    const active = root.activeElement;
    const listLoaded = googleFamilies.size > 0 || localFamilies.size > 0;
    for (const g of groupOrder) {
      const {input} = combos[g];
      if (input !== active) input.value = value(g, 'font');
      input.classList.toggle('unknown', listLoaded && !resolve(input.value));
      input.classList.toggle('changed', fk(g, 'font') in overrides);

      for (const [key, input] of Object.entries(inputs[g])) {
        input.classList.toggle('changed', fk(g, key) in overrides || (key === 'chars' && 'width' in overrides));
        if (input === active) continue;
        const v = key === 'chars' ? value('body', 'width') / avgCharWidth() : value(g, key);
        input.value = v === undefined ? ''
                    : key === 'chars' ? Math.round(v)
                    : fmt(v, fieldDefs[key].digits);
      }

      for (const [axis, name, auto] of [['wdth', 'Width', 'font default'], ['opsz', 'Optical size', 'auto (follows font size)']]) {
        const range = axisRange(g, axis);
        inputs[g][axis].parentNode.classList.toggle('inactive', !range);
        inputs[g][axis].parentNode.title = range
          ? `${name}, font supports ${range[0]}–${range[1]}. Empty = ${auto}`
          : `Current font has no ${name.toLowerCase()} axis`;
      }
    }
    for (const [option, button] of Object.entries(alignButtons)) {
      button.classList.toggle('on', value('body', 'align') === option);
    }
    inputs.body.margin.parentNode.classList.toggle('inactive', value('body', 'align') !== 'left');
  }

  function update() {
    save();
    apply();
    refresh();
  }

  function setNumber(g, key, n) {
    if (key === 'chars') {
      key = 'width';
      n = Math.round(n * avgCharWidth());
    }
    const {min} = fieldDefs[key];
    if (isFinite(n) && (min === undefined || n >= min)) {
      overrides[fk(g, key)] = n;
    } else {
      delete overrides[fk(g, key)];
    }
    update();
  }

  function setAlign(option) {
    if (option === defaults.body.align) {
      delete overrides.align;
    } else {
      overrides.align = option;
    }
    update();
  }

  /** UI **/

  const panelCss = `
    :host { all: initial; position: fixed; top: 0; left: 0; right: 0; z-index: 2147483647; }
    .panel { display: flex; align-items: flex-start; gap: 10px; padding: 5px 10px; background: #FFF; color: #000;
             border-bottom: 1px solid rgba(0,0,0,0.2);
             font: 12px/1 system-ui, -apple-system, sans-serif; font-variant-numeric: tabular-nums; cursor: default; }
    .grid { flex-grow: 1; display: grid; grid-template-columns: auto repeat(${columns.length}, auto) 1fr auto; gap: 4px 10px; align-items: center; justify-items: start; }
    .row-label { color: rgba(0,0,0,0.5); font-weight: 600; }
    .actions { display: flex; gap: 6px; }
    label { display: flex; align-items: center; gap: 3px; color: rgba(0,0,0,0.5); }
    label.inactive { opacity: 0.4; }
    input { font: inherit; color: #000; width: 4em; padding: 3px 4px; border: 1px solid rgba(0,0,0,0.2); border-radius: 3px; background: #FFF; outline: none; }
    input:focus { border-color: #4A90E2; box-shadow: 0 0 0 2px rgba(74,144,226,0.3); }
    input.font { width: 14em; }
    input.unknown { color: #C00; }
    input.changed { background: #FFF6C8; }
    .combo { position: relative; display: flex; }
    .list { position: absolute; z-index: 10; top: calc(100% + 3px); left: 0; min-width: 100%; max-height: 60vh; overflow-y: auto; padding: 2px 0;
            background: #FFF; border: 1px solid rgba(0,0,0,0.2); border-radius: 3px; box-shadow: 0 4px 12px rgba(0,0,0,0.2); }
    .list[hidden] { display: none; }
    .item { padding: 4px 8px; white-space: nowrap; cursor: pointer; }
    .item.on { background: #4A90E2; color: #FFF; }
    button { font: inherit; color: #000; padding: 3px 6px; border: 1px solid rgba(0,0,0,0.2); border-radius: 3px; background: #F4F4F4; cursor: pointer; }
    button:hover { background: #E8E8E8; }
    .toggle { display: flex; }
    .toggle > button { border-radius: 0; }
    .toggle > button:first-child { border-radius: 3px 0 0 3px; }
    .toggle > button:last-child { border-radius: 0 3px 3px 0; margin-left: -1px; }
    .toggle > button.on { background: #4A90E2; border-color: #4A90E2; color: #FFF; }
  `;

  function onKeyDown(e, g, key) {
    if (e.key !== 'ArrowUp' && e.key !== 'ArrowDown') return;
    e.preventDefault();
    const def = fieldDefs[key];
    const step = e.shiftKey ? def.step * 10 : def.step;
    // Empty 'auto' field starts from what auto would use
    const current = e.target.value === '' && def.auto ? (key === 'opsz' ? value(g, 'size') : 100) : parseFloat(e.target.value);
    const v = Math.round(current / step * 1000) / 1000 || 0;
    const n = Math.round((e.key === 'ArrowUp' ? Math.floor(v) + 1 : Math.ceil(v) - 1) * step * 1e6) / 1e6;
    if (def.min !== undefined && n < def.min) return;
    e.target.value = fmt(n, def.digits);
    setNumber(g, key, n);
  }

  // Font input with dropdown: arrows and hover preview, Enter/click chooses, Escape restores
  function fontCombo(g) {
    const key = fk(g, 'font');
    const input = el('input', {className: 'font', spellcheck: false, placeholder: 'Font', autocomplete: 'off'});
    const list = el('div', {className: 'list', hidden: true});
    let matches = [];      // names currently shown
    let index = -1;        // highlighted item
    let before;            // font when dropdown opened, restored on Escape
    let keepSelection = false;
    let lastMouse;

    function show(query) {
      if (list.hidden) {
        before = overrides[key];
        list.hidden = false;
      }
      const q = query.trim().toLowerCase();
      matches = q
        ? [...fontNames.filter(n => n.toLowerCase().startsWith(q)),
           ...fontNames.filter(n => !n.toLowerCase().startsWith(q) && n.toLowerCase().includes(q))]
        : fontNames;
      index = -1;
      list.replaceChildren(...matches.map((name, i) => el('div', {className: 'item', textContent: name, dataset: {i}})));
    }

    // Full list with the current font highlighted
    function showAll() {
      show('');
      highlight(matches.indexOf(value(g, 'font')), true);
    }

    function hide() {
      list.hidden = true;
      index = -1;
    }

    function highlight(i, scroll) {
      list.children[index]?.classList.remove('on');
      index = i;
      const item = list.children[i];
      if (!item) return;
      item.classList.add('on');
      if (scroll) item.scrollIntoView({block: 'nearest'});
    }

    function previewAt(i, scroll) {
      if (i < 0 || i >= matches.length) return;
      highlight(i, scroll);
      preview = {g, name: matches[i]};
      apply();
      refresh();
    }

    function choose(name) {
      preview = null;
      if (name) {
        overrides[key] = name;
      } else {
        delete overrides[key];
      }
      hide();
      update();
      input.value = value(g, 'font');
    }

    input.addEventListener('focus', () => {
      input.select();
      keepSelection = true; // the click that focused the input would deselect on mouseup
      showAll();
    });
    input.addEventListener('mouseup', (e) => {
      if (keepSelection) e.preventDefault();
      keepSelection = false;
    });
    input.addEventListener('click', () => {
      // Already focused (e.g. right after choosing a font): reopen like a fresh focus
      if (list.hidden) {
        input.select();
        showAll();
      }
    });
    input.addEventListener('input', () => {
      preview = null;
      show(input.value);
      const name = input.value.trim();
      if (name === '') {
        delete overrides[key];
      } else if (resolve(name)) {
        overrides[key] = resolve(name).name;
      } else {
        // Unknown font: wait for Enter to try it as an installed system font
        refresh();
        return;
      }
      update();
    });
    input.addEventListener('keydown', (e) => {
      if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
        e.preventDefault();
        if (list.hidden) showAll();
        previewAt(e.key === 'ArrowDown' ? index + 1 : Math.max(index - 1, 0), true);
      } else if (e.key === 'Enter') {
        if (!list.hidden && index >= 0) {
          choose(matches[index]);
        } else if (input.value.trim() !== '') {
          // Not in the list: try it as an installed system font
          choose(input.value.trim());
        }
      } else if (e.key === 'Escape' && !list.hidden) {
        choose(before);
      }
    });
    input.addEventListener('blur', () => {
      // Clicking away keeps what's previewed; only Escape restores
      if (preview?.g === g) choose(preview.name);
      hide();
      setTimeout(refresh);
    });

    list.addEventListener('mousedown', (e) => e.preventDefault()); // keep focus in the input
    list.addEventListener('mousemove', (e) => {
      // Ignore mousemove fired by scrolling the list under a still pointer
      if (lastMouse && lastMouse.x === e.screenX && lastMouse.y === e.screenY) return;
      lastMouse = {x: e.screenX, y: e.screenY};
      const item = e.target.closest('.item');
      if (item && Number(item.dataset.i) !== index) previewAt(Number(item.dataset.i), false);
    });
    list.addEventListener('click', (e) => {
      const item = e.target.closest('.item');
      if (item) choose(matches[Number(item.dataset.i)]);
    });

    combos[g] = {input};
    return el('div', {className: 'combo'}, input, list);
  }

  function field(g, key) {
    if (key === 'font') {
      return fontCombo(g);
    }
    const def = fieldDefs[key];
    if (def.options) {
      const toggle = el('div', {className: 'toggle'});
      for (const option of def.options) {
        const button = el('button', {textContent: option[0].toUpperCase() + option.slice(1)});
        button.addEventListener('click', () => setAlign(option));
        alignButtons[option] = button;
        toggle.appendChild(button);
      }
      return toggle;
    }
    const input = el('input', {placeholder: def.auto ? 'auto' : ''});
    input.addEventListener('input', () => setNumber(g, key, parseFloat(input.value)));
    input.addEventListener('keydown', (e) => onKeyDown(e, g, key));
    input.addEventListener('blur', () => setTimeout(refresh));
    inputs[g][key] = input;
    const label = el('label', {title: key === 'chars' ? 'Average characters per line at this width' : ''}, def.label, input);
    if (def.unit) label.append(def.unit);
    return label;
  }

  function copyCss() {
    const text = css(true);
    console.log(text);
    navigator.clipboard?.writeText(text);
  }

  function reset() {
    overrides = {};
    update();
  }

  function build() {
    const host = el('div', {id: 'font-tuner'});
    root = host.attachShadow({mode: 'open'});
    root.appendChild(el('style', {textContent: panelCss}));

    const panel = el('div', {className: 'panel'});

    const grid = el('div', {className: 'grid'});
    for (const [row, g] of groupOrder.entries()) {
      grid.append(el('span', {className: 'row-label', textContent: groups[g].label}));
      for (const column of columns) {
        const key = groups[g].fields.find(k => columnOf(k) === column);
        grid.append(key ? field(g, key) : el('span'));
      }
      grid.append(el('span')); // spacer
      if (row === 0) {
        const actions = el('div', {className: 'actions'});
        for (const [text, title, handler] of [['Copy', 'Copy overrides as CSS', copyCss], ['Reset', 'Reset all overrides', reset]]) {
          const button = el('button', {textContent: text, title});
          button.addEventListener('click', handler);
          actions.appendChild(button);
        }
        grid.append(actions);
      } else {
        grid.append(el('span'));
      }
    }

    panel.append(grid);
    root.appendChild(panel);
    document.body.appendChild(host);

    // Push the page down so the panel sits above it instead of covering it
    new ResizeObserver(() => {
      document.documentElement.style.paddingTop = panel.offsetHeight + 'px';
    }).observe(panel);
  }

  /** INIT **/

  readDefaults();
  styleEl = el('style', {id: 'font-tuner-style'});
  document.head.appendChild(styleEl);
  build();
  apply();
  refresh();
  document.fonts.addEventListener('loadingdone', refresh);
  fetchFamilies();
})();
