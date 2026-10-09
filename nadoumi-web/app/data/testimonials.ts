/**
 * Student stories shown on the home page. The quotes are the students' own words, kept in the language they wrote
 * them in (`lang`), and are not translated. Photos are in `public/students`.
 */
export interface Testimonial {
  id: string
  image: string
  /** ISO 3166 region of the student's home country and of the country they study in. */
  from: string
  to: string
  /** Null when the student shared no name: the card then shows a generic label. */
  name: string | null
  lang: 'en' | 'fr'
  quote: string
}

export const testimonials: Testimonial[] = [
  {
    id: 'chad-en',
    image: '/students/student-1.jpg',
    from: 'TD',
    to: 'CN',
    name: null,
    lang: 'en',
    quote: 'I would like to express my deepest gratitude to NADOUMI for the incredible opportunity they gave me to come to China. This experience means much more to me than simply traveling to another country. It has been a true opportunity to discover the world, learn, grow, and experience a new culture.\n\nThanks to NADOUMI, I have been able to begin a new chapter of my life and live an experience that I may never have had the chance to experience otherwise. Coming to China has allowed me to step outside my comfort zone, meet new people, discover a completely different environment, and continue my studies in a place that is teaching me so much, both academically and personally.\n\nMy journey here is still ongoing, and I am grateful to have the opportunity to continue learning, growing, and gradually building my future through this experience.\n\nI am truly grateful for the trust that was placed in me and for this opportunity, which is already contributing so much to my journey. Thank you to the entire NADOUMI team for their support and for making this adventure possible.\n\nFrom the bottom of my heart, thank you, NADOUMI. 🇹🇩❤️🇨🇳',
  },
  {
    id: 'chad-fr',
    image: '/students/student-2.jpg',
    from: 'TD',
    to: 'CN',
    name: null,
    lang: 'fr',
    quote: 'Je tiens à exprimer ma profonde gratitude envers l’entreprise NADOUMI pour l’opportunité exceptionnelle qu’elle m’a offerte de venir en Chine. Cette expérience représente pour moi bien plus qu’un simple voyage : c’est une véritable ouverture sur le monde, une occasion d’apprendre, de grandir et de découvrir une nouvelle culture.\n\nGrâce à NADOUMI, j’ai pu commencer une nouvelle étape de ma vie et vivre une expérience que je n’aurais peut-être jamais eu la chance de connaître. Venir en Chine me permet de sortir de ma zone de confort, de rencontrer de nouvelles personnes, de découvrir un environnement différent et de poursuivre mes études dans un cadre qui m’apporte énormément, aussi bien sur le plan académique que personnel.\n\nMon parcours ici est encore en cours, et je suis heureuse de pouvoir continuer à apprendre, à évoluer et à construire petit à petit mon avenir grâce à cette opportunité.\n\nJe suis sincèrement reconnaissante pour la confiance qui m’a été accordée et pour cette chance qui contribue déjà énormément à mon parcours. Merci à toute l’équipe de NADOUMI pour son accompagnement et pour avoir rendu cette aventure possible.\n\nDu fond du cœur, merci NADOUMI. 🇹🇩❤️🇨🇳',
  },
  {
    id: 'cameroon-en',
    image: '/students/student-3.jpg',
    from: 'CM',
    to: 'CN',
    name: 'Fru Schinaylla',
    lang: 'en',
    quote: 'I am FRU SCHINAYLLA from Cameroon, and I sincerely appreciate NADOUMI Edu & Services for being an important part of my journey from Cameroon to China. 🇨🇲➡️🇨🇳\n\nFrom the beginning, they guided me through the process and made everything much easier. Their support didn’t stop after I arrived in China they continued to check on me and even welcomed me with a thoughtful gift. 🥹❤️\n\nThank you, NADOUMI Edu & Services, for your care, support, and kindness throughout my journey. I’m truly grateful for everything you’ve done for me! ❤️🇨🇲➡️🇨🇳',
  },
]
