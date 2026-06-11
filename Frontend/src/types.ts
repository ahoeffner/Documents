export interface Category
{
  id: number
  name: string
}


export interface Folder
{
  id: number
  name: string
  pid: number | null
  children: Folder[]
}


export interface DocumentResult
{
  id: number
  title: string
  filename?: string
  date: string
  description?: string
  hasFile: boolean
  fldid?: number | null
  isLink?: boolean
  linkId?: number | null
}


export interface Language
{
  id: string
  name: string
}


export interface ChatMessage
{
  id: string
  role: 'user' | 'ai'
  text: string
  documents: DocumentResult[] | null
  timestamp: Date
}


export interface DocumentDetail
{
  id: number
  date: string
  title: string
  text?: string
  fldid?: number | null
  filename?: string | null
  hasFile: boolean
  url?: string | null
}


export interface SearchPayload
{
  query: string
  folder: number
}


export interface ChatPayload
{
  id: string
  query: string
  folder: number
  match: number
}
