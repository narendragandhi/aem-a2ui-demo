export interface ContentSuggestion {
  id: string;
  title: string;
  subtitle?: string;
  description: string;
  ctaText: string;
  ctaUrl: string;
  imageUrl: string;
  imageAlt?: string;
  componentType: string;
  price?: string;
  visualScore?: number;
  selectedAssetId?: string;
  // SEO related properties
  seo?: SeoSuggestions;
  seoScore?: number;
  // Additional component-specific properties
  cta?: { text: string; url: string };
  items?: any[];
  quote?: string;
  author?: string;
  images?: string[];
}

export interface SeoSuggestions {
  metaTitle?: string;
  metaDescription?: string;
  keywords?: string[];
  ogTitle?: string;
  ogDescription?: string;
  schemaType?: string;
  readabilityScore?: number;
}

export interface ImageAsset {
  id: string;
  url: string;
  thumbnailUrl?: string;
  name: string;
  tags: string[];
  category: AssetCategory;
  dimensions?: {
    width: number;
    height: number;
  };
  format?: string;
  size?: number;
  brandAligned?: boolean;
  description?: string;
  createdAt?: string;
}

export type AssetCategory =
  | 'hero'
  | 'product'
  | 'lifestyle'
  | 'abstract'
  | 'team'
  | 'office'
  | 'technology'
  | 'nature'
  | 'illustration';

export interface AssetFilter {
  search?: string;
  category?: AssetCategory | 'all';
  tags?: string[];
  brandAligned?: boolean;
}
