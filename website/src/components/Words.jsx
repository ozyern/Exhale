/**
 * A heading that fills in a word at a time.
 *
 * The reference's section titles arrive grey and light up as they cross into
 * view. Doing it per word rather than per line is what makes it read as the
 * line being *written* instead of a block changing color — and it costs one
 * span each, with the stagger carried in a custom property so the whole thing
 * still runs off the page's single reveal observer.
 */
export default function Words({ text, className = '', step = 55 }) {
  return (
    <span className={`words reveal ${className}`.trim()}>
      {text.split(' ').map((word, index) => (
        <span key={`${word}-${index}`} style={{ '--d': `${index * step}ms` }}>
          {word}
          {index < text.split(' ').length - 1 ? ' ' : ''}
        </span>
      ))}
    </span>
  )
}
