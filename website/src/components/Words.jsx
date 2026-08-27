/**
 * A heading that fills in a word at a time. One span each, staggered through a
 * custom property so it still runs off the page's single reveal observer.
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
