/*
 * Five build facts, each drawn with the thing it came out of. A number on its
 * own is a claim; the same number next to its source is evidence.
 *
 * Values come from app/build.gradle.kts. If the build changes and this doesn't,
 * the page is wrong.
 */

const Sdk = () => (
  <div className="s-art s-sdk">
    {[
      ['37', 'compileSdk'],
      ['36', 'targetSdk'],
      ['33', 'minSdk'],
    ].map(([value, label], i) => (
      <div key={label} data-lead={i === 2}>
        <b>{value}</b>
        <span>{label}</span>
      </div>
    ))}
  </div>
)

const Kotlin = () => (
  <pre className="s-art s-code">
    <span className="s-an">@Composable</span>
    {'\n'}
    <span className="s-kw">fun</span> <span className="s-fn">ExhaleDock</span>({'\n'}
    {'  '}active: <span className="s-ty">Int</span>,{'\n'}
    {'  '}backdrop: <span className="s-ty">Backdrop</span>,{'\n'}
    {') {'}
    {'\n'}
    {'  '}
    <span className="s-fn">Row</span>(Modifier.<span className="s-fn">drawBackdrop</span>({'\n'}
    {'    '}backdrop,
    {'\n'}
    {'  '}) {'{ '}
    <span className="s-fn">lens</span>() {'}'}){'\n'}
    {'}'}
  </pre>
)

const License = () => (
  <div className="s-art s-doc">
    <b>GNU GENERAL PUBLIC LICENSE</b>
    <span>Version 3, 29 June 2007</span>
    <div className="s-lines" aria-hidden="true">
      {[96, 88, 92, 70, 84, 90, 62].map((w, i) => (
        <i key={i} style={{ width: `${w}%` }} />
      ))}
    </div>
  </div>
)

const Abi = () => (
  <div className="s-art s-abi">
    {/* Highlighted is what we publish. The rest are flavours the project still
        defines, reachable if you build it yourself. */}
    {[
      ['universal', true],
      ['arm64-v8a', false],
      ['armeabi-v7a', false],
      ['x86_64', false],
      ['x86', false],
    ].map(([name, on]) => (
      <span key={name} data-on={on}>
        {name}
      </span>
    ))}
  </div>
)

const Release = () => (
  <div className="s-art s-release">
    <b>1.0.102</b>
    <span>versionCode 102</span>
    <div className="s-tagrow">
      <span className="s-tag">GitHub Releases</span>
    </div>
  </div>
)

const ART = { sdk: Sdk, kotlin: Kotlin, license: License, abi: Abi, release: Release }

export default function Specs({ items }) {
  return (
    <div className="cardrow">
      {items.map((spec, i) => {
        const Art = ART[spec.art]
        return (
          <figure className="scard reveal" key={spec.value} style={{ '--d': `${i * 70}ms` }}>
            <div className="scard-art">{Art ? <Art /> : null}</div>
            <figcaption>
              <b>{spec.value}.</b> {spec.body}
            </figcaption>
          </figure>
        )
      })}
    </div>
  )
}
